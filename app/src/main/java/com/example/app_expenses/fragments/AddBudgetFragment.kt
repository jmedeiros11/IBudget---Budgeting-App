package com.example.app_expenses.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_expenses.R
import com.example.app_expenses.activities.MainActivity
import com.example.app_expenses.adapters.CategoryAdapter
import com.example.app_expenses.adapters.MyBudgetsAdapter
import com.example.app_expenses.data.BudgetData
import com.example.app_expenses.data.MyBudgetData
import com.example.app_expenses.databinding.FragmentAddBudgetBinding
import com.example.app_expenses.enums.CategoryEnum
import com.example.app_expenses.utils.PrefsHelper
import com.example.app_expenses.utils.StringUtils
import com.example.app_expenses.utils.UtilitiesFunctions
import com.example.app_expenses.viewModels.BudgetsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddBudgetFragment(private val budgetsAdapter: MyBudgetsAdapter): Fragment() {
    private lateinit var fragmentAddBudgetBinding: FragmentAddBudgetBinding
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var categories: MutableList<CategoryEnum>
    private var mainActivity: MainActivity = MainActivity()
    private val budgetsViewModel: BudgetsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        fragmentAddBudgetBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_add_budget,
            container, false
        )
        categories = createCategories()
        categoryAdapter = CategoryAdapter(categories)
        fragmentAddBudgetBinding.recyclerViewAddBudget.adapter = categoryAdapter
        fragmentAddBudgetBinding.recyclerViewAddBudget.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        return fragmentAddBudgetBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        budgetsViewModel.getAddBudgetLiveData().observe(viewLifecycleOwner){ newBudget ->
            if(newBudget != null){
                val total = newBudget.budgetAmount!!.toFloat() + PrefsHelper.readFloat(StringUtils.TOTAL_BUDGET)!!
                PrefsHelper.writeFloat(StringUtils.TOTAL_BUDGET, total)
                val item = MyBudgetData(UtilitiesFunctions.getCategoryEnum(newBudget.categoryName!!),
                    newBudget.budgetName, newBudget.budgetAmount)
                budgetsAdapter.addItem(item, 0)
                budgetsViewModel.addToTotalBudget(newBudget.budgetAmount!!.toFloat())
                Toast.makeText(context, "Budget has been created successfully!", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
                mainActivity.visibleTabBarVisibility()
            } else{
                Toast.makeText(context, "Error. User not registered.", Toast.LENGTH_LONG).show()
            }
        }

        fragmentAddBudgetBinding.addBudgetBackButton.setOnClickListener {
            parentFragmentManager.popBackStack()
            mainActivity.visibleTabBarVisibility()
        }

        fragmentAddBudgetBinding.btnConfirmAddBudget.setOnClickListener {
            fragmentAddBudgetBinding.etBudgetName.clearFocus()
            fragmentAddBudgetBinding.etAmount.clearFocus()
            if(validateFields() && categoryAdapter.isCategorySelected.value == true){
                val selectedCategory = categories[categoryAdapter.rowIndex].categoryName
                val budgetName = fragmentAddBudgetBinding.etBudgetName.text.toString()
                val budgetAmount = fragmentAddBudgetBinding.etAmount.text.toString()
                val newBudget = BudgetData(selectedCategory, budgetName, budgetAmount)
                budgetsViewModel.addBudget(newBudget)
            }
        }
    }

    private fun createCategories(): MutableList<CategoryEnum>{
        val categories = mutableListOf<CategoryEnum>()
        categories.add(CategoryEnum.GROCERIES)
        categories.add(CategoryEnum.ENTERTAINMENT)
        categories.add(CategoryEnum.TRANSPORTATION)
        categories.add(CategoryEnum.SUBSCRIPTIONS)
        categories.add(CategoryEnum.BILLS)
        categories.add(CategoryEnum.PERSONAL_SPENDING)
        return categories
    }

    private fun validateFields(): Boolean{
        var isValidName = false
        var isValidAmount = false
        if(TextUtils.isEmpty(fragmentAddBudgetBinding.etBudgetName.text.toString())){
            fragmentAddBudgetBinding.tvInvalidBudgetName.visibility = View.VISIBLE
            fragmentAddBudgetBinding.etBudgetName.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red_bright))
            fragmentAddBudgetBinding.etBudgetName.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_bright))
            fragmentAddBudgetBinding.etBudgetName.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(
                requireContext(), R.color.red_bright))
            fragmentAddBudgetBinding.tvBudgetNameTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_bright))
            fragmentAddBudgetBinding.tvInvalidBudgetName.text = resources.getString(R.string.invalid_budget_name)
        } else if(!validateUniqueName(fragmentAddBudgetBinding.etBudgetName.text.toString())){
            fragmentAddBudgetBinding.tvInvalidBudgetName.visibility = View.VISIBLE
            fragmentAddBudgetBinding.etBudgetName.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red_bright))
            fragmentAddBudgetBinding.etBudgetName.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_bright))
            fragmentAddBudgetBinding.etBudgetName.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(
                requireContext(), R.color.red_bright))
            fragmentAddBudgetBinding.tvBudgetNameTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_bright))
            fragmentAddBudgetBinding.tvInvalidBudgetName.text = resources.getString(R.string.duplicate_budget_name)
        } else{
            isValidName = true
        }
        if (TextUtils.isEmpty(fragmentAddBudgetBinding.etAmount.text.toString()) || !validateAmount(
                fragmentAddBudgetBinding.etAmount.text.toString())) {
            fragmentAddBudgetBinding.tvInvalidBudgetAmount.visibility = View.VISIBLE
            fragmentAddBudgetBinding.etAmount.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red_bright))
            fragmentAddBudgetBinding.etAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_bright))
            fragmentAddBudgetBinding.etAmount.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(
                requireContext(), R.color.red_bright))
            fragmentAddBudgetBinding.tvBudgetAmountTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_bright))
        }else{
            isValidAmount = true
        }

        if(categoryAdapter.isCategorySelected.value == false){
            fragmentAddBudgetBinding.tvInvalidCategorySelected.visibility = View.VISIBLE
        }

        resetInvalidFields()

        return isValidAmount && isValidName
    }

    private fun validateAmount(amount: String?): Boolean{
        amount?.let {
            val amountPattern = "^[0-9]+[.][0-9][0-9]\$"
            val amountMatcher = Regex(amountPattern)
            return amountMatcher.find(it) != null
        } ?: return false
    }

    private fun validateUniqueName(typedBudgetName: String): Boolean{
        var isUnique = true
        for(budget in budgetsAdapter.listOfBudgets){
            if(budget.budgetName!! == typedBudgetName){
                isUnique = false
                break
            }
        }
        return isUnique
    }

    private fun resetInvalidFields(){
        fragmentAddBudgetBinding.etBudgetName.setOnFocusChangeListener { _, _ ->
            fragmentAddBudgetBinding.tvInvalidBudgetName.visibility = View.GONE
            fragmentAddBudgetBinding.etBudgetName.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.background_tertiary))
            fragmentAddBudgetBinding.etBudgetName.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            fragmentAddBudgetBinding.etBudgetName.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(
                requireContext(), R.color.foreground_primary))
            fragmentAddBudgetBinding.tvBudgetNameTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.foreground_primary))
        }

        fragmentAddBudgetBinding.etAmount.setOnFocusChangeListener { _, _ ->
            fragmentAddBudgetBinding.tvInvalidBudgetAmount.visibility = View.GONE
            fragmentAddBudgetBinding.etAmount.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.background_tertiary))
            fragmentAddBudgetBinding.etAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            fragmentAddBudgetBinding.etAmount.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(
                requireContext(), R.color.foreground_primary))
            fragmentAddBudgetBinding.tvBudgetAmountTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.foreground_primary))
        }

        lifecycleScope.launch(Dispatchers.Main){
            categoryAdapter.isCategorySelected.observe(requireActivity()){ selected ->
                    if(selected){
                        fragmentAddBudgetBinding.tvInvalidCategorySelected.visibility = View.GONE
                    }
            }
        }

    }


}