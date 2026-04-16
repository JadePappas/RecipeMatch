package com.example.recipematch.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private val tag = "HomeFragment"
    private lateinit var userViewModel: UserViewModel
    private lateinit var pantryViewModel: PantryViewModel
    private lateinit var discoverViewModel: DiscoverViewModel
    private lateinit var albumViewModel: AlbumViewModel
    private lateinit var recommendedAdapter: RecipeAdapter
    private lateinit var detailContainer: FrameLayout

    private val attemptRepo = RecipeAttemptRepository()
    private val auth = FirebaseAuth.getInstance()
    private var currentAttempt: RecipeAttempt? = null

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
        else Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.home_fragment, container, false)
        
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        pantryViewModel = ViewModelProvider(requireActivity()).get(PantryViewModel::class.java)
        discoverViewModel = ViewModelProvider(requireActivity()).get(DiscoverViewModel::class.java)
        albumViewModel = ViewModelProvider(requireActivity()).get(AlbumViewModel::class.java)

        val tvGreeting = view.findViewById<TextView>(R.id.tv_greeting)
        val tvItemsAvailable = view.findViewById<TextView>(R.id.tv_items_available)
        val tvRecipesFound = view.findViewById<TextView>(R.id.tv_recipes_found)
        val btnUpdatePantry = view.findViewById<Button>(R.id.btn_update_pantry)
        val rvRecommended = view.findViewById<RecyclerView>(R.id.rv_recommended)
        val btnSeeAll = view.findViewById<TextView>(R.id.see_all_recommended)
        detailContainer = view.findViewById(R.id.home_recipe_detail_container)

        rvRecommended.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recommendedAdapter = RecipeAdapter { recipe -> showRecipeDetail(recipe) }
        rvRecommended.adapter = recommendedAdapter

        userViewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let { tvGreeting.text = "Hi, ${it.displayName.ifEmpty { it.username }}!" }
        }

        pantryViewModel.pantryItems.observe(viewLifecycleOwner) { items ->
            tvItemsAvailable.text = "You have ${items.size} items available"
            recommendedAdapter.updateUserData(items, pantryViewModel.equipment.value ?: emptyList())
            sortAndSubmitRecommended()
            updateMatchCount()
        }

        pantryViewModel.equipment.observe(viewLifecycleOwner) { equipment ->
            recommendedAdapter.updateUserData(pantryViewModel.pantryItems.value ?: emptyList(), equipment)
            sortAndSubmitRecommended()
            updateMatchCount()
        }

        discoverViewModel.homeRecipes.observe(viewLifecycleOwner) { 
            sortAndSubmitRecommended()
            updateMatchCount()
        }

        btnUpdatePantry.setOnClickListener { navigateToTab(R.id.navigation_pantry) }
        btnSeeAll.setOnClickListener { navigateToTab(R.id.navigation_discover) }

        setupExploreButtons(view)
        discoverViewModel.fetchHomeRecipes()

        return view
    }

    private fun sortAndSubmitRecommended() {
        val recipes = discoverViewModel.homeRecipes.value ?: return
        val userIngs = pantryViewModel.pantryItems.value ?: emptyList()
        val userEqs = pantryViewModel.equipment.value ?: emptyList()
        val sortedList = recipes.sortedByDescending { calculateMatchPercentage(it, userIngs, userEqs) }
        recommendedAdapter.submitList(sortedList.take(5))
    }

    private fun updateMatchCount() {
        val recipes = discoverViewModel.homeRecipes.value ?: return
        val userIngs = pantryViewModel.pantryItems.value ?: emptyList()
        val userEqs = pantryViewModel.equipment.value ?: emptyList()

        if (recipes.isEmpty()) {
            view?.findViewById<TextView>(R.id.tv_recipes_found)?.text = "Finding recipes you can make..."
            return
        }

        val recipeMatchPairs = recipes.map { it to calculateMatchPercentage(it, userIngs, userEqs) }
            .sortedByDescending { it.second }
        
        val top3Matches = recipeMatchPairs.take(3)
        val count = top3Matches.size

        val tvFound = view?.findViewById<TextView>(R.id.tv_recipes_found)
        tvFound?.text = "We found your top $count matches →"

        tvFound?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BestMatchesFragment.newInstance(top3Matches.map { it.first }))
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupExploreButtons(view: View) {
        view.findViewById<Chip>(R.id.chip_breakfast).setOnClickListener { searchAndNavigate("Breakfast") }
        view.findViewById<Chip>(R.id.chip_lunch).setOnClickListener { searchAndNavigate("Lunch") }
        view.findViewById<Chip>(R.id.chip_dinner).setOnClickListener { searchAndNavigate("Dinner") }
        view.findViewById<Chip>(R.id.chip_healthy).setOnClickListener { searchAndNavigate("Healthy") }
        view.findViewById<Chip>(R.id.chip_dessert).setOnClickListener { searchAndNavigate("Dessert") }
        view.findViewById<Chip>(R.id.chip_sweet).setOnClickListener { searchAndNavigate("Sweet") }
    }

    private fun searchAndNavigate(category: String) {
        discoverViewModel.searchRecipes(query = category)
        navigateToTab(R.id.navigation_discover)
    }

    private fun navigateToTab(itemId: Int) {
        val nav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        nav.selectedItemId = itemId
    }

    private fun showRecipeDetail(recipe: Recipe) {
        detailContainer.visibility = View.VISIBLE
        val detailView = layoutInflater.inflate(R.layout.recipe_detail_fragment, detailContainer, false)
        detailContainer.removeAllViews()
        detailContainer.addView(detailView)

        currentAttempt = null
        currentPhotoUri = null

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
        val btnSaveNotes = detailView.findViewById<ImageButton>(R.id.btn_save_notes)

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

        btnSaveNotes.setOnClickListener {
            if (currentAttempt != null) {
                saveNewAttempt(recipe, btnSaveAttempt) // Re-saves with updated notes
                Toast.makeText(context, "Notes updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Mark as completed to save notes!", Toast.LENGTH_SHORT).show()
            }
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

    private fun populateDetails(recipe: Recipe, ingContainer: LinearLayout, eqContainer: LinearLayout, insContainer: LinearLayout) {
        if (recipe.extendedIngredients != null) {
            ingContainer.removeAllViews()
            recipe.extendedIngredients.forEach { ingredient ->
                val item = layoutInflater.inflate(R.layout.item_recipe_ingredient, ingContainer, false)
                item.findViewById<TextView>(R.id.tv_ingredient_name).text = ingredient.original
                val btnStatus = item.findViewById<Button>(R.id.btn_ingredient_status)
                val userHas = pantryViewModel.pantryItems.value?.any { ingredient.name.contains(it.ingredientName, true) || it.ingredientName.contains(ingredient.name, true) } ?: false
                btnStatus.text = if (userHas) "In Stock" else "Need to buy"
                btnStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), if (userHas) android.R.color.holo_green_light else android.R.color.holo_red_light))
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
            btnStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), if (userHas) android.R.color.holo_green_light else android.R.color.holo_red_light))
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
}