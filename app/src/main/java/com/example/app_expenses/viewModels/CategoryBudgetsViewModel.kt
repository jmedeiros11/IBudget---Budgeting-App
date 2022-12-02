package com.example.app_expenses.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.app_expenses.data.BudgetCategoryData
import com.example.app_expenses.repositories.CategoryBudgetsRepository

class CategoryBudgetsViewModel: ViewModel() {
    private val categoryBudgetsListRepository = CategoryBudgetsRepository()
    private val categoryTotalBudgetLiveData: LiveData<BudgetCategoryData> = categoryBudgetsListRepository.getCategoryTotalBudgetLiveData()
    private val categoryBudgetsLiveData:  LiveData<List<BudgetCategoryData>> = categoryBudgetsListRepository.getCategoryBudgetsLiveData()

    fun getCategoryBudgets(){
        categoryBudgetsListRepository.getCategoryBudgets()
    }

    fun getCategoryBudgetsLiveData(): LiveData<List<BudgetCategoryData>> {
        return categoryBudgetsLiveData
    }

    fun subtractFromCategoryTotalBudget(budgetCategoryData: BudgetCategoryData){
        categoryBudgetsListRepository.subtractFromCategoryBudget(budgetCategoryData)
    }

    fun addToCategoryTotalBudget(budgetCategoryData: BudgetCategoryData){
        categoryBudgetsListRepository.addToCategoryBudget(budgetCategoryData)
    }

    fun getCategoryTotalBudget(categoryName: String){
        categoryBudgetsListRepository.getCategoryTotalBudget(categoryName)
    }

    fun getCategoryTotalBudgetLiveData(): LiveData<BudgetCategoryData>{
        return categoryTotalBudgetLiveData
    }

}