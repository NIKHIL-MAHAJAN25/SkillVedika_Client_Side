package com.nikhil.buyerapp.basichome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import retrofit2.Callback
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.BuildConfig
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.FragmentHome2Binding
import com.nikhil.buyerapp.dataclasses.FreelancerItem
import com.nikhil.buyerapp.freelancesearch.FreelanceAdapter
import com.nikhil.buyerapp.news.NewsAdapter
import com.nikhil.buyerapp.news.NewsResponse
import com.nikhil.buyerapp.news.RetroNews
import com.nikhil.buyerapp.utils.logd
import com.nikhil.buyerapp.utils.loge
import com.nikhil.buyerapp.utils.snack
import retrofit2.Call
import retrofit2.Response
import androidx.activity.OnBackPressedCallback



// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var _binding:FragmentHome2Binding?=null

    private val binding get() =_binding!!
    lateinit var serviceAdapter: ServiceAdapter
    lateinit var newsAdapter: NewsAdapter
    lateinit var freeshow: FreelanceAdapter
    private var firestoreListener:ListenerRegistration?=null
    private val auth:FirebaseAuth=FirebaseAuth.getInstance()
    private val db=Firebase.firestore
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=FragmentHome2Binding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check if the search results are currently visible
                if (binding.rvSearchResults.visibility == View.VISIBLE) {

                    // 1. Clear the search text (this triggers your TextWatcher to show home content)
                    binding.etsearchbar.text?.clear()

                    // 2. Hide the search UI manually just in case
                    toggleSearch(false)

                    // 3. Clear focus and hide keyboard
                    hideKeyboard()

                } else {
                    // If we aren't searching, disable this callback and let the system handle the back press
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        setup()
        setupnews()
        loadinfo()
        fetchnews()
        ///////////////////////////////////////////search logic//////////////////////////////////////////////////
        binding.etsearchbar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()

                if (query.isNotEmpty()) {
                    toggleSearch(true)  // <--- Hides the Group, shows Search RV
                    performNameSearch(query)
                } else {
                    toggleSearch(false) // <--- Shows the Group back
                    loadinfo()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

// 3. Handle the "Search" button click on the keyboard
        binding.etsearchbar.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard when search is pressed
                hideKeyboard()
                true
            } else {
                false
            }
        }
        ///////////////////////////////////////////search logic//////////////////////////////////////////////////




    }
    private fun fetchnews()
    {
        val apikey="${BuildConfig.NEWS_KEY}"
        val query="Artificial Intelligence, Machine learning"

        RetroNews.instance.getnews(apikey,query,size=8).enqueue(object : Callback<NewsResponse>{
            override fun onResponse(call: Call<NewsResponse>, response: Response<NewsResponse>) {
               if(_binding==null) return
                if(response.isSuccessful)
                {
                    val rawList = response.body()?.results

                    // --- THE BOUNCER (Filtering Logic) ---
                    val cleanList = rawList?.filter { article ->
                        // Rule: "You can only enter if you have a non-empty Image URL"
                        !article.image_url.isNullOrBlank()
                    }

                    if(!cleanList.isNullOrEmpty())
                    {
                        newsAdapter.submitList(cleanList)
                    }else{
                        loge("News error:${response.code()}")
                    }
                }
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                if (_binding == null) return
                loge("News API Failed", t)
                snack("Failed to load news")
            }

        })
    }
    private fun setupnews()
    {
        newsAdapter=NewsAdapter { link->
            try{
                val intent=Intent(Intent.ACTION_VIEW, Uri.parse(link))
                startActivity(intent)
            }catch (e:Exception)
            {
                loge("could not open link",e)
            }
        }
        binding.recyclernews.apply{
            adapter=newsAdapter
            layoutManager=LinearLayoutManager(requireContext())
            isNestedScrollingEnabled=false
        }
    }
    private fun setup(){
        freeshow = FreelanceAdapter { freelancer ->
            // Handle click on a freelancer from search results
            // Example: navigate to their profile
        }
        binding.rvSearchResults.apply {
            adapter = freeshow
            layoutManager = LinearLayoutManager(requireContext())
        }
        serviceAdapter= ServiceAdapter { onclicked ->
            val bundle= Bundle().apply {
                putString("categoryprimskill",onclicked.title)
            }
                findNavController().navigate(
                    R.id.freelancesearch,bundle
                )
        }
        binding.recyclerservices.apply {
            adapter=serviceAdapter
        }
    }
    private fun loadinfo()
    {
        val uid=auth.currentUser?.uid
        firestoreListener?.remove()
        firestoreListener=db.collection("Skills").addSnapshotListener { snapshot, error ->
            if(error!=null)
            {
                loge("listen failed")
                return@addSnapshotListener
            }
//            lifecycle check
            if(_binding==null)
            {
                return@addSnapshotListener
            }
            if(snapshot!=null && !snapshot.isEmpty())
            {
                val skill=snapshot.toObjects(DataSkill::class.java)
                serviceAdapter.submitList(skill)
            }else{
                logd("null or empty")
                serviceAdapter.submitList(emptyList())

            }
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    private fun performNameSearch(query: String) {

        // Note: This searches your "Skills" collection by title based on your existing code
        db.collection("Freelancers")
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .get()
            .addOnSuccessListener { freelancerDocs ->
                val freelancerList = mutableListOf<FreelancerItem>()

                // If no freelancers found, just update and return
                if (freelancerDocs.isEmpty) {
                    freeshow.submitList(emptyList())
                    return@addOnSuccessListener
                }

                // Loop through each freelancer to "Join" their User Data
                for (doc in freelancerDocs) {
                    val freelancer = doc.toObject(FreelancerItem::class.java)
                    val uid = doc.id // Assuming document ID is the User UID

                    // Fetch the image from the "Users" collection
                    db.collection("Users").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            val imageUrl = userDoc.getString("profilePictureUrl") ?: ""

                            // Set the URL manually into your object
                            freelancer.profileImageUrl = imageUrl

                            freelancerList.add(freelancer)

                            // Once we have processed all documents, submit the list
                            if (freelancerList.size == freelancerDocs.size()) {
                                freeshow.submitList(freelancerList.toList())
                            }
                        }
                }
            }
    }
    private fun toggleSearch(isSearching: Boolean) {
        if (isSearching) {
            binding.homeContentGroup.visibility = View.GONE
            binding.rvSearchResults.visibility = View.VISIBLE
        } else {
            binding.homeContentGroup.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
        }
    }
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
        _binding=null
    }

}