package com.example.recipematch.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipematch.R
import com.example.recipematch.model.Album
import com.example.recipematch.model.Recipe
import com.example.recipematch.model.RecipeAttempt
import com.example.recipematch.repository.RecipeAttemptRepository
import com.example.recipematch.viewmodel.AlbumViewModel
import com.example.recipematch.viewmodel.DiscoverViewModel
import com.example.recipematch.viewmodel.PantryViewModel
import com.example.recipematch.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AlbumDetailFragment : Fragment() {

    private lateinit var albumViewModel: AlbumViewModel
    private lateinit var pantryViewModel: PantryViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var discoverViewModel: DiscoverViewModel
    private lateinit var recipeAdapter: RecipeAdapter
    
    private val attemptRepo = RecipeAttemptRepository()
    private val auth = FirebaseAuth.getInstance()
    private var currentAttempt: RecipeAttempt? = null
    
    private lateinit var detailContainer: FrameLayout
    private var albumId: String? = null
    private var albumName: String? = null
    private var recipeIds: List<String> = emptyList()

    private var currentPhotoUri: Uri? = null
    private var ivAttemptPreview: ImageView? = null
    private var photoPlaceholder: View? = null
    private var etAttemptNotes: EditText? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updatePhotoPreview(currentPhotoUri)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera()
    }

    companion object {
        fun newInstance(album: Album): AlbumDetailFragment {
            val fragment = AlbumDetailFragment()
            val args = Bundle()
            args.putString("album_id", album.albumId)
            args.putString("album_name", album.albumName)
            args.putStringArrayList("recipe_ids", ArrayList(album.recipes))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        albumId = arguments?.getString("album_id")
        albumName = arguments?.getString("album_name")
        recipeIds = arguments?.getStringArrayList("recipe_ids") ?: emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.album_detail_fragment, container, false)
        albumViewModel = ViewModelProvider(requireActivity()).get(AlbumViewModel::class.java)
        pantryViewModel = ViewModelProvider(requireActivity()).get(PantryViewModel::class.java)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        discoverViewModel = ViewModelProvider(requireActivity()).get(DiscoverViewModel::class.java)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_album_back)
        val btnDelete = view.findViewById<ImageButton>(R.id.btn_delete_album)
        val tvTitle = view.findViewById<TextView>(R.id.tv_album_detail_title)
        val rvRecipes = view.findViewById<RecyclerView>(R.id.rv_album_recipes)
        detailContainer = view.findViewById(R.id.album_recipe_detail_container)

        tvTitle.text = albumName ?: "Album Details"
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        rvRecipes.layoutManager = GridLayoutManager(requireContext(), 2)
        recipeAdapter = RecipeAdapter { recipe -> showRecipeDetail(recipe) }
        rvRecipes.adapter = recipeAdapter

        pantryViewModel.pantryItems.observe(viewLifecycleOwner) { items ->
            recipeAdapter.updateUserData(items, pantryViewModel.equipment.value ?: emptyList())
        }

        albumViewModel.albumRecipes.observe(viewLifecycleOwner) { recipes ->
            recipeAdapter.submitList(recipes)
        }

        if (recipeIds.isNotEmpty()) albumViewModel.fetchRecipesForAlbum(recipeIds)

        return view
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Album")
            .setMessage("Are you sure you want to delete this album? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                albumId?.let {
                    albumViewModel.deleteAlbum(it)
                    parentFragmentManager.popBackStack()
                    Toast.makeText(context, "Album deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRecipeDetail(recipe: Recipe) {
        detailContainer.visibility = View.VISIBLE
        val detailView = layoutInflater.inflate(R.layout.recipe_detail_fragment, detailContainer, false)
        detailContainer.removeAllViews()
        detailContainer.addView(detailView)

        val btnBack = detailView.findViewById<ImageButton>(R.id.btn_back)
        val btnSaveToAlbum = detailView.findViewById<Button>(R.id.btn_save_to_album)
        val btnSaveAttempt = detailView.findViewById<Button>(R.id.btn_save_attempt)
        ivAttemptPreview = detailView.findViewById(R.id.iv_attempt_photo)
        photoPlaceholder = detailView.findViewById(R.id.ll_add_photo_placeholder)
        etAttemptNotes = detailView.findViewById(R.id.et_attempt_notes)
        val cvAddPhoto = detailView.findViewById<View>(R.id.cv_add_photo)

        btnBack.setOnClickListener { detailContainer.visibility = View.GONE }
        btnSaveToAlbum.setOnClickListener { showAlbumSelectionDialog(recipe) }
        cvAddPhoto.setOnClickListener { checkCameraPermission() }

        // Fetch existing attempt
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                currentAttempt = attemptRepo.getRecipeAttempt(userId, recipe.id.toString())
                if (currentAttempt != null) {
                    setCompletedUI(btnSaveAttempt)
                    etAttemptNotes?.setText(currentAttempt!!.notes)
                    if (currentAttempt!!.photoUri.isNotEmpty()) updatePhotoPreview(Uri.parse(currentAttempt!!.photoUri))
                } else {
                    setUncompletedUI(btnSaveAttempt)
                }
            }
        }

        btnSaveAttempt.setOnClickListener {
            if (currentAttempt == null) saveNewAttempt(recipe, btnSaveAttempt)
            else deleteAttempt(btnSaveAttempt)
        }

        detailView.findViewById<TextView>(R.id.tv_detail_recipe_name).text = recipe.title
        detailView.findViewById<TextView>(R.id.tv_detail_time).text = "${recipe.readyInMinutes} min"
        detailView.findViewById<TextView>(R.id.tv_detail_servings).text = recipe.servings.toString()
        detailView.findViewById<TextView>(R.id.tv_detail_rating).text = "★ ${String.format("%.1f", recipe.spoonacularScore / 20.0)}"
        Glide.with(this).load(recipe.image).into(detailView.findViewById(R.id.iv_detail_image))

        populateIngredientsAndInstructions(recipe, detailView)
    }

    private fun showAlbumSelectionDialog(recipe: Recipe) {
        val albums = albumViewModel.albums.value ?: emptyList()
        if (albums.isEmpty()) {
            Toast.makeText(context, "No albums found. Create one in Profile!", Toast.LENGTH_SHORT).show()
            return
        }
        val albumNames = albums.map { it.albumName }.toTypedArray()
        AlertDialog.Builder(requireContext()).setTitle("Select Album").setItems(albumNames) { _, which ->
            albumViewModel.addRecipeToAlbum(albums[which], recipe.id.toString(), recipe.image)
            Toast.makeText(context, "Saved to ${albums[which].albumName}", Toast.LENGTH_SHORT).show()
        }.setNegativeButton("Cancel", null).show()
    }

    private fun saveNewAttempt(recipe: Recipe, button: Button) {
        val userId = auth.currentUser?.uid ?: return
        val attempt = RecipeAttempt(
            userId = userId, recipeApiId = recipe.id.toString(), recipeTitle = recipe.title,
            notes = etAttemptNotes?.text.toString(), photoUri = currentPhotoUri?.toString() ?: "",
            dateCompleted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        attemptRepo.saveRecipeAttempt(attempt) { success ->
            if (success) {
                userViewModel.rewardExperience(100)
                setCompletedUI(button)
                Toast.makeText(context, "Recipe Completed! +100 XP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteAttempt(button: Button) {
        val attemptId = currentAttempt?.id ?: return
        attemptRepo.deleteRecipeAttempt(attemptId) { success ->
            if (success) {
                setUncompletedUI(button)
                Toast.makeText(context, "Attempt unmarked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setCompletedUI(button: Button) {
        button.text = "Completed"
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
    }

    private fun setUncompletedUI(button: Button) {
        button.text = "Mark as Completed"
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
    }

    private fun updatePhotoPreview(uri: Uri?) {
        if (uri != null) {
            ivAttemptPreview?.visibility = View.VISIBLE
            photoPlaceholder?.visibility = View.GONE
            ivAttemptPreview?.setImageURI(uri)
            currentPhotoUri = uri
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        val photoFile = File(requireContext().filesDir, "attempt_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri) }
        cameraLauncher.launch(intent)
    }

    private fun populateIngredientsAndInstructions(recipe: Recipe, view: View) {
        val ingContainer = view.findViewById<LinearLayout>(R.id.ll_ingredients_container)
        val insContainer = view.findViewById<LinearLayout>(R.id.ll_instructions_container)
        recipe.extendedIngredients?.forEach { ingredient ->
            val item = layoutInflater.inflate(R.layout.item_recipe_ingredient, ingContainer, false)
            item.findViewById<TextView>(R.id.tv_ingredient_name).text = ingredient.original
            val btnStatus = item.findViewById<Button>(R.id.btn_ingredient_status)
            val userHas = pantryViewModel.pantryItems.value?.any { ingredient.name.contains(it.ingredientName, true) || it.ingredientName.contains(ingredient.name, true) } ?: false
            btnStatus.text = if (userHas) "In Kitchen" else "Need to buy"
            btnStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), if (userHas) android.R.color.holo_green_light else android.R.color.holo_red_light))
            ingContainer.addView(item)
        }
        recipe.analyzedInstructions?.firstOrNull()?.steps?.forEach { step ->
            val stepView = layoutInflater.inflate(R.layout.item_instruction_step, insContainer, false)
            stepView.findViewById<TextView>(R.id.tv_step_number).text = "${step.number}."
            stepView.findViewById<TextView>(R.id.tv_step_description).text = step.step
            insContainer.addView(stepView)
        }
    }
}