package com.example.recipematch.ui

import android.Manifest
import android.app.Activity
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
import com.example.recipematch.model.Recipe
import com.example.recipematch.model.RecipeAttempt
import com.example.recipematch.repository.RecipeAttemptRepository
import com.example.recipematch.viewmodel.DiscoverViewModel
import com.example.recipematch.viewmodel.PantryViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DiscoverFragment : Fragment() {

    private lateinit var discoverViewModel: DiscoverViewModel
    private lateinit var pantryViewModel: PantryViewModel
    private lateinit var recipeAdapter: RecipeAdapter
    
    private lateinit var progressBar: ProgressBar
    private lateinit var detailContainer: FrameLayout
    private lateinit var rvRecipes: RecyclerView

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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.discover_fragment, container, false)
        discoverViewModel = ViewModelProvider(this).get(DiscoverViewModel::class.java)
        pantryViewModel = ViewModelProvider(requireActivity()).get(PantryViewModel::class.java)

        progressBar = view.findViewById(R.id.pb_discover_loading)
        detailContainer = view.findViewById(R.id.recipe_detail_container)
        rvRecipes = view.findViewById(R.id.rv_discover_recipes)

        rvRecipes.layoutManager = GridLayoutManager(requireContext(), 2)
        recipeAdapter = RecipeAdapter { recipe -> showRecipeDetail(recipe) }
        rvRecipes.adapter = recipeAdapter

        pantryViewModel.pantryItems.observe(viewLifecycleOwner) { items ->
            recipeAdapter.updateUserData(items, pantryViewModel.equipment.value ?: emptyList())
        }
        pantryViewModel.equipment.observe(viewLifecycleOwner) { equipment ->
            recipeAdapter.updateUserData(pantryViewModel.pantryItems.value ?: emptyList(), equipment)
        }

        view.findViewById<ImageButton>(R.id.btn_search_discover).setOnClickListener {
            discoverViewModel.searchRecipes(query = view.findViewById<EditText>(R.id.et_search_recipe).text.toString())
        }

        view.findViewById<ChipGroup>(R.id.cg_cuisine_filters).setOnCheckedStateChangeListener { group, checkedIds ->
            val cuisine = if (checkedIds.isEmpty()) null else {
                val chip = group.findViewById<Chip>(checkedIds[0])
                if (chip.text == "All") null else chip.text.toString()
            }
            discoverViewModel.searchRecipes(cuisine = cuisine)
        }

        discoverViewModel.recipes.observe(viewLifecycleOwner) { recipeAdapter.submitList(it) }
        discoverViewModel.isLoading.observe(viewLifecycleOwner) { 
            progressBar.visibility = if (it) View.VISIBLE else View.GONE 
        }

        discoverViewModel.searchRecipes()
        return view
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
        val instructionsContainer = detailView.findViewById<LinearLayout>(R.id.ll_instructions_container)

        ivAttemptPreview = detailView.findViewById(R.id.iv_attempt_photo)
        photoPlaceholder = detailView.findViewById(R.id.ll_add_photo_placeholder)
        etAttemptNotes = detailView.findViewById(R.id.et_attempt_notes)
        val cvAddPhoto = detailView.findViewById<View>(R.id.cv_add_photo)
        val btnSaveAttempt = detailView.findViewById<Button>(R.id.btn_save_attempt)

        btnBack.setOnClickListener { detailContainer.visibility = View.GONE }
        
        cvAddPhoto.setOnClickListener {
            if (currentPhotoUri != null) {
                showPhotoPopup(currentPhotoUri!!)
            } else {
                checkCameraPermission()
            }
        }

        // Fetch existing attempt
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                currentAttempt = attemptRepo.getRecipeAttempt(userId, recipe.id.toString())
                if (currentAttempt != null) {
                    setCompletedUI(btnSaveAttempt)
                    etAttemptNotes?.setText(currentAttempt!!.notes)
                    if (currentAttempt!!.photoUri.isNotEmpty()) {
                        updatePhotoPreview(Uri.parse(currentAttempt!!.photoUri))
                    }
                } else {
                    setUncompletedUI(btnSaveAttempt)
                }
            }
        }

        btnSaveAttempt.setOnClickListener {
            if (currentAttempt == null) {
                saveNewAttempt(recipe.id, btnSaveAttempt)
            } else {
                deleteAttempt(btnSaveAttempt)
            }
        }

        tvName.text = recipe.title
        tvTime.text = "${recipe.readyInMinutes} min"
        tvServings.text = recipe.servings.toString()
        tvRating.text = "★ ${String.format("%.1f", recipe.spoonacularScore / 20.0)}"
        Glide.with(this).load(recipe.image).into(ivImage)

        populateDetails(recipe, ingredientsContainer, instructionsContainer)
        discoverViewModel.selectedRecipe.observe(viewLifecycleOwner) { updated ->
            if (updated?.id == recipe.id) populateDetails(updated, ingredientsContainer, instructionsContainer)
        }
        discoverViewModel.getRecipeDetails(recipe.id)
    }

    private fun showPhotoPopup(uri: Uri) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_full_photo)
        
        val imageView = dialog.findViewById<ImageView>(R.id.iv_full_photo)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btn_close_popup)
        val btnRetake = dialog.findViewById<Button>(R.id.btn_retake_photo)
        
        imageView.setImageURI(uri)
        
        btnClose.setOnClickListener { dialog.dismiss() }
        btnRetake.setOnClickListener {
            dialog.dismiss()
            checkCameraPermission()
        }
        
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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

    private fun saveNewAttempt(recipeId: Int, button: Button) {
        val userId = auth.currentUser?.uid ?: return
        val attempt = RecipeAttempt(
            userId = userId,
            recipeApiId = recipeId.toString(),
            notes = etAttemptNotes?.text.toString(),
            photoUri = currentPhotoUri?.toString() ?: "",
            dateCompleted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        attemptRepo.saveRecipeAttempt(attempt) { success ->
            if (success) {
                viewLifecycleOwner.lifecycleScope.launch {
                    currentAttempt = attemptRepo.getRecipeAttempt(userId, recipeId.toString())
                    setCompletedUI(button)
                    Toast.makeText(context, "Attempt saved!", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Attempt unmarked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateDetails(recipe: Recipe, ingContainer: LinearLayout, insContainer: LinearLayout) {
        if (recipe.extendedIngredients != null) {
            ingContainer.removeAllViews()
            recipe.extendedIngredients.forEach { ingredient ->
                val item = layoutInflater.inflate(R.layout.item_recipe_ingredient, ingContainer, false)
                item.findViewById<TextView>(R.id.tv_ingredient_name).text = ingredient.original
                val btnStatus = item.findViewById<Button>(R.id.btn_ingredient_status)
                val userHas = pantryViewModel.pantryItems.value?.any { 
                    ingredient.name.contains(it.ingredientName, true) || it.ingredientName.contains(ingredient.name, true)
                } ?: false
                btnStatus.text = if (userHas) "In Stock" else "Need to buy"
                btnStatus.setBackgroundColor(resources.getColor(if (userHas) android.R.color.holo_green_light else android.R.color.holo_red_light, null))
                ingContainer.addView(item)
            }
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