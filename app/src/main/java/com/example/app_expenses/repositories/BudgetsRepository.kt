package com.example.app_expenses.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.app_expenses.data.BudgetData
import com.example.app_expenses.data.MyBudgetData
import com.example.app_expenses.enums.CategoryEnum
import com.example.app_expenses.utils.UtilitiesFunctions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class BudgetsRepository {
    private val firebaseDatabase: DatabaseReference = Firebase.database.reference
    private val auth: FirebaseAuth = Firebase.auth
    private val myBudgetsLiveData = MutableLiveData<List<MyBudgetData>>()
    private val addBudgetLiveData = MutableLiveData<BudgetData?>()
    private val totalBudgetLiveData = MutableLiveData<Float>()

    fun getMyBudgets() {
        CoroutineScope(Dispatchers.IO).launch {
            val myBudgetsList: MutableList<MyBudgetData> = mutableListOf()
            firebaseDatabase
                .child("users").child(auth.currentUser?.uid!!).child("budgets")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (dataSnapshot1 in dataSnapshot.children) {
                                val budgetName = dataSnapshot1.child("budgetName").value as String
                                val budgetAmount =
                                    dataSnapshot1.child("budgetAmount").value as String
                                val budgetCategory =
                                    dataSnapshot1.child("categoryName").value as String
                                val category: CategoryEnum =
                                    (UtilitiesFunctions.getCategoryEnum(budgetCategory) ?: null) as CategoryEnum
                                myBudgetsList.add(MyBudgetData(category, budgetName, budgetAmount))
                            }
                            myBudgetsLiveData.postValue(myBudgetsList)
                        }
                    }
                    override fun onCancelled(databaseError: DatabaseError) {}
                })
        }
    }
    fun getMyBudgetsLiveData(): LiveData<List<MyBudgetData>>{
        return myBudgetsLiveData
    }

    fun addBudget(newBudget: BudgetData){
        CoroutineScope(Dispatchers.IO).launch {
            firebaseDatabase.child("users").child(auth.currentUser?.uid!!).child("budgets").push().setValue(newBudget).addOnCompleteListener { registerUser ->
                if(registerUser.isSuccessful){
                    addBudgetLiveData.postValue(newBudget)
                }
                else{
                    addBudgetLiveData.postValue(null)
                }
            }
        }
    }

    fun getAddBudgetLiveData(): LiveData<BudgetData?>{
        return addBudgetLiveData
    }

    fun removeBudget(budgetName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            firebaseDatabase.child("users").child(auth.currentUser?.uid!!).child("budgets")
                .orderByChild("budgetName").equalTo(budgetName)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for(data in snapshot.children){
                            data.ref.removeValue()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    fun addToTotalBudget(){
        CoroutineScope(Dispatchers.IO).launch {
            firebaseDatabase
                .child("users").child(auth.currentUser?.uid!!).child("budgets").limitToLast(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for(data in dataSnapshot.children){
                            val myBudgetData = data.getValue(MyBudgetData::class.java)
                            totalBudgetAddSubtract(myBudgetData!!, true)
                        }
                    }
                    override fun onCancelled(databaseError: DatabaseError) {}
                })
        }
    }

    fun removeFromTotalBudget(){
        CoroutineScope(Dispatchers.IO).launch {
            firebaseDatabase.child("users").child(auth.currentUser?.uid!!).child("budgets").addChildEventListener(object :
                ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {}

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val myBudgetData = snapshot.getValue(MyBudgetData::class.java)
                    totalBudgetAddSubtract(myBudgetData!!, false)
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    fun getTotalBudget(){
        CoroutineScope(Dispatchers.IO).launch {
            firebaseDatabase
                .child("users").child(auth.currentUser?.uid!!).child("total_budget")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            val totalBudget = dataSnapshot.getValue(String::class.java)!!.toFloat()
                            totalBudgetLiveData.postValue(totalBudget)
                        }
                    }
                    override fun onCancelled(databaseError: DatabaseError) {}
                })
        }
    }

    fun getTotalBudgetLiveData(): LiveData<Float>{
        return totalBudgetLiveData
    }

    private fun totalBudgetAddSubtract(myBudgetData: MyBudgetData, isAdding: Boolean){
        val totalBudget =  firebaseDatabase.child("users").child(auth.currentUser?.uid!!).child("total_budget")
        totalBudget.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var newTotalBudget: Float
                val oldTotalBudget = snapshot.value as String?
                if(oldTotalBudget == null){
                    newTotalBudget = myBudgetData!!.budgetAmount!!.toFloat()
                } else{
                    newTotalBudget = oldTotalBudget!!.toFloat() - myBudgetData!!.budgetAmount!!.toFloat()
                    if(isAdding){
                        newTotalBudget = oldTotalBudget!!.toFloat() + myBudgetData!!.budgetAmount!!.toFloat()
                    }
                }
                totalBudget.setValue(newTotalBudget.toString())
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}