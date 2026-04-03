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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
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
import com.example.recipematch.model.Recipe
import com.example.recipematch.model.RecipeAttempt
import com.example.recipematch.repository.RecipeAttemptRepository
import com.example.recipematch.viewmodel.AlbumViewModel
import com.example.recipematch.viewmodel.DiscoverViewModel
import com.example.recipematch.viewmodel.PantryViewModel
import com.example.recipematch.viewmodel.UserViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DiscoverFragment : Fragment() {

    private val tag = "DiscoverFragment"
    private lateinit var discoverViewModel: DiscoverViewModel
    private lateinit var pantryViewModel: PantryViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var albumViewModel: AlbumViewModel
    private lateinit var recipeAdapter: RecipeAdapter
    
    private lateinit var progressBar: ProgressBar
    private lateinit var detailContainer: FrameLayout
    private lateinit var rvRecipes: RecyclerView

    private val attemptRepo = RecipeAttemptRepository()
    private val auth = FirebaseAuth.getInstance()
    private var currentAttempt: RecipeAttempt? = null

    // RESTORED: These variables are needed for the camera and photo preview to work
    private var currentPhotoUri: Uri? = null
    private var ivAttemptPreview: ImageView? = null
    private var photoPlaceholder: View? = null
    private var etAttemptNotes: EditText? = null

    // Track the last processed list to handle incremental sorting
    private var lastProcessedRecipes: List<Recipe> = emptyList()

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updatePhotoPreview(currentPhotoUri)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera()
        else Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.discover_fragment, container, false)
        
        discoverViewModel = ViewModelProvider(requireActivity()).get(DiscoverViewModel::class.java)
        pantryViewModel = ViewModelProvider(requireActivity()).get(PantryViewModel::class.java)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        albumViewModel = ViewModelProvider(requireActivity()).get(AlbumViewModel::class.java)

        progressBar = view.findViewById(R.id.pb_discover_loading)
        detailContainer = view.findViewById(R.id.recipe_detail_container)
        rvRecipes = view.findViewById(R.id.rv_discover_recipes)

        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        rvRecipes.layoutManager = gridLayoutManager
        recipeAdapter = RecipeAdapter { recipe -> showRecipeDetail(recipe) }
        rvRecipes.adapter = recipeAdapter

        rvRecipes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val visibleItemCount = gridLayoutManager.childCount
                    val totalItemCount = gridLayoutManager.itemCount
                    val firstVisibleItemPosition = gridLayoutManager.findFirstVisibleItemPosition()

                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0) {
                        discoverViewModel.searchRecipes(isLoadMore = true)
                    }
                }
            }
        })

        pantryViewModel.pantryItems.observe(viewLifecycleOwner) { items ->
            recipeAdapter.updateUserData(items, pantryViewModel.equipment.value ?: emptyList())
            sortAndSubmitRecipes(forceFullSort = true)
        }
        pantryViewModel.equipment.observe(viewLifecycleOwner) { equipment ->
            recipeAdapter.updateUserData(pantryViewModel.pantryItems.value ?: emptyList(), equipment)
            sortAndSubmitRecipes(forceFullSort = true)
        }

        view.findViewById<ImageButton>(R.id.btn_search_discover).setOnClickListener {
            lastProcessedRecipes = emptyList()
            rvRecipes.scrollToPosition(0)
            discoverViewModel.searchRecipes(query = view.findViewById<EditText>(R.id.et_search_recipe).text.toString())
        }

        view.findViewById<ChipGroup>(R.id.cg_cuisine_filters).setOnCheckedStateChangeListener { group, checkedIds ->
            val cuisine = if (checkedIds.isEmpty()) null else {
                val chip = group.findViewById<Chip>(checkedIds[0])
                if (chip.text == "All") null else chip.text.toString()
            }
            lastProcessedRecipes = emptyList()
            rvRecipes.scrollToPosition(0)
            discoverViewModel.searchRecipes(cuisine = cuisine)
        }

        discoverViewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            sortAndSubmitRecipes(newRecipesFromViewModel = recipes)
        }
        
        discoverViewModel.isLoading.observe(viewLifecycleOwner) { 
            progressBar.visibility = if (it) View.VISIBLE else View.GONE 
        }

        if (discoverViewModel.recipes.value.isNullOrEmpty()) {
            discoverViewModel.searchRecipes()
        }
        
        return view
    }

    private fun sortAndSubmitRecipes(newRecipesFromViewModel: List<Recipe>? = null, forceFullSort: Boolean = false) {
        val allRecipes = newRecipesFromViewModel ?: discoverViewModel.recipes.value ?: return
        val userIngs = pantryViewModel.pantryItems.value ?: emptyList()
        val userEqs = pantryViewModel.equipment.value ?: emptyList()

        if (forceFullSort || lastProcessedRecipes.isEmpty()) {
            val sortedList = allRecipes.sortedByDescending { calculateMatchPercentage(it, userIngs, userEqs) }
            lastProcessedRecipes = sortedList
            recipeAdapter.submitList(sortedList)
        } else if (allRecipes.size > lastProcessedRecipes.size) {
            val newBatch = allRecipes.subList(lastProcessedRecipes.size, allRecipes.size)
            val sortedNewBatch = newBatch.sortedByDescending { calculateMatchPercentage(it, userIngs, userEqs) }
            val updatedList = lastProcessedRecipes + sortedNewBatch
            lastProcessedRecipes = updatedList
            recipeAdapter.submitList(updatedList)
        }
    }

    private fun calculateMatchPercentage(recipe: Recipe, userIngs: List<com.example.recipematch.model.PantryItem>, userEqs: List<com.example.recipematch.model.UserEquipment>): Int {
        val recipeIngredients = recipe.extendedIngredients ?: emptyList()
        val recipeEquipment = recipe.analyzedInstructions?.flatMap { it.steps }?.flatMap { it.equipment ?: emptyList() }?.distinctBy { it.id } ?: emptyList()
        if (recipeIngredients.isEmpty() && recipeEquipment.isEmpty()) return 0
        var matchedCount = 0
        recipeIngredients.forEach { recipeIng ->
            if (userIngs.any { it.ingredientName.contains(recipeIng.name, true) || recipeIng.name.contains(it.ingredientName, true) }) matchedCount++
        }
        recipeEquipment.forEach { reqEq ->
            if (userEqs.any { it.equipmentName.contains(reqEq.name, true) || reqEq.name.contains(it.equipmentName, true) }) matchedCount++
        }
        val totalRequired = recipeIngredients.size + recipeEquipment.size
        return if (totalRequired > 0) (matchedCount * 100) / totalRequired else 0
    }

    private fun showRecipeDetail(recipe: Recipe) {
        detailContainer.visibility = View.VISIBLE
        val detailView = layoutInflater.inflate(R.layout.recipe_detail_fragment, detailContainer, false)
        detailContainer.removeAllViews()
        detailContainer.addView(detailView)

        val btnBack = detailView.findViewById<ImageButton>(R.id.btn_back)
        val tvName = detailView.findViewById<TextView>(R.id.tv_detail_recipe_name)
        val ivImage = detailView.findViewById<ImageView>(R.id.iv_detail_image)
        val tvTime = detailView.findViewById<TextView>(R.id.tv_detail_time)
        val tvServings = detailView.findViewById<TextView>(R.id.tv_detail_servings)
        val tvRating = detailView.findViewById<TextView>(R.id.tv_detail_rating)
        val ingredientsContainer = detailView.findViewById<LinearLayout>(R.id.ll_ingredients_container)
        val equipmentContainer = detailView.findViewById<LinearLayout>(R.id.ll_equipment_container)
        val instructionsContainer = detailView.findViewById<LinearLayout>(R.id.ll_instructions_container)

        ivAttemptPreview = detailView.findViewById(R.id.iv_attempt_photo)
        photoPlaceholder = detailView.findViewById(R.id.ll_add_photo_placeholder)
        etAttemptNotes = detailView.findViewById(R.id.et_attempt_notes)
        val cvAddPhoto = detailView.findViewById<View>(R.id.cv_add_photo)
        val btnSaveAttempt = detailView.findViewById<Button>(R.id.btn_save_attempt)
        val btnSaveToAlbum = detailView.findViewById<Button>(R.id.btn_save_to_album)

        btnBack.setOnClickListener { detailContainer.visibility = View.GONE }
        btnSaveToAlbum.setOnClickListener { showAlbumSelectionDialog(recipe) }
        cvAddPhoto.setOnClickListener { checkCameraPermission() }

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
                    etAttemptNotes?.setText("")
                }
            }
        }

        btnSaveAttempt.setOnClickListener {
            if (currentAttempt == null) saveNewAttempt(recipe, btnSaveAttempt)
            else showUnmarkConfirmation(btnSaveAttempt)
        }

        tvName.text = recipe.title
        tvTime.text = "${recipe.readyInMinutes} min"
        tvServings.text = recipe.servings.toString()
        tvRating.text = "★ ${String.format("%.1f", recipe.spoonacularScore / 20.0)}"
        Glide.with(this).load(recipe.image).into(ivImage)

        populateDetails(recipe, ingredientsContainer, equipmentContainer, instructionsContainer)
        discoverViewModel.selectedRecipe.observe(viewLifecycleOwner) { updated ->
            if (updated?.id == recipe.id) populateDetails(updated, ingredientsContainer, equipmentContainer, instructionsContainer)
        }
        discoverViewModel.getRecipeDetails(recipe.id)
    }

    private fun showAlbumSelectionDialog(recipe: Recipe) {
        val albums = albumViewModel.albums.value ?: emptyList()
        if (albums.isEmpty()) {
            Toast.makeText(context, "No albums found. Create one in Profile!", Toast.LENGTH_SHORT).show()
            return
        }
        val albumNames = albums.map { it.albumName }.toTypedArray()
        AlertDialog.Builder(requireContext()).setTitle("Select Album").setItems(albumNames) { _, which ->
            val selectedAlbum = albums[which]
            albumViewModel.addRecipeToAlbum(selectedAlbum, recipe.id.toString(), recipe.image)
            Toast.makeText(context, "Saved to ${selectedAlbum.albumName}", Toast.LENGTH_SHORT).show()
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showUnmarkConfirmation(button: Button) {
        AlertDialog.Builder(requireContext())
            .setTitle("Unmark as Completed?")
            .setMessage("This will permanently delete your photo and notes for this attempt.")
            .setPositiveButton("Unmark") { _, _ -> deleteAttempt(button) }
            .setNegativeButton("Cancel", null)
            .show()
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
                viewLifecycleOwner.lifecycleScope.launch {
                    currentAttempt = attemptRepo.getRecipeAttempt(userId, recipe.id.toString())
                    setCompletedUI(button)
                    Toast.makeText(context, "Recipe Completed! +100 XP", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteAttempt(button: Button) {
        val attemptId = currentAttempt?.id ?: return
        attemptRepo.deleteRecipeAttempt(attemptId) { success ->
            if (success) {
                currentAttempt = null
                currentPhotoUri = null
                ivAttemptPreview?.visibility = View.GONE
                photoPlaceholder?.visibility = View.VISIBLE
                etAttemptNotes?.setText("")
                setUncompletedUI(button)
                Toast.makeText(context, "Attempt deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateDetails(recipe: Recipe, ingContainer: LinearLayout, eqContainer: LinearLayout, insContainer: LinearLayout) {
        if (recipe.extendedIngredients != null) {
            ingContainer.removeAllViews()
            recipe.extendedIngredients.forEach { ingredient ->
                val item = layoutInflater.inflate(R.layout.item_recipe_ingredient, ingContainer, false)
                item.findViewById<TextView>(R.id.tv_ingredient_name).text = ingredient.original
                val btnStatus = item.findViewById<Button>(R.id.btn_ingredient_status)
                val userHas = pantryViewModel.pantryItems.value?.any { ingredient.name.contains(it.ingredientName, true) || it.ingredientName.contains(ingredient.name, true) } ?: false
                btnStatus.text = if (userHas) "In Stock" else "Need to buy"
                btnStatus.setBackgroundColor(resources.getColor(if (userHas) android.R.color.holo_green_light else android.R.color.holo_red_light, null))
                ingContainer.addView(item)
            }
        }
        val recipeEquipment = recipe.analyzedInstructions?.flatMap { it.steps }?.flatMap { it.equipment ?: emptyList() }?.distinctBy { it.id } ?: emptyList()
        eqContainer.removeAllViews()
        recipeEquipment.forEach { eq ->
            val item = layoutInflater.inflate(R.layout.item_recipe_ingredient, eqContainer, false)
            item.findViewById<TextView>(R.id.tv_ingredient_name).text = eq.name.replaceFirstChar { it.uppercase() }
            val btnStatus = item.findViewById<Button>(R.id.btn_ingredient_status)
            val userHas = pantryViewModel.equipment.value?.any { eq.name.contains(it.equipmentName, true) || it.equipmentName.contains(eq.name, true) } ?: false
            btnStatus.text = if (userHas) "In Kitchen" else "Missing"
            btnStatus.setBackgroundColor(resources.getColor(if (userHas) android.R.color.holo_green_light else android.R.color.holo_red_light, null))
            eqContainer.addView(item)
        }
        insContainer.removeAllViews()
        recipe.analyzedInstructions?.firstOrNull()?.steps?.forEach { step ->
            val stepView = layoutInflater.inflate(R.layout.item_instruction_step, insContainer, false)
            stepView.findViewById<TextView>(R.id.tv_step_number).text = "${step.number}."
            stepView.findViewById<TextView>(R.id.tv_step_description).text = step.step
            insContainer.addView(stepView)
        }
    }
}