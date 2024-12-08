package com.lion.a08_memoapplication.fragment

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.android.material.snackbar.Snackbar
import com.lion.a08_memoapplication.MainActivity
import com.lion.a08_memoapplication.R
import com.lion.a08_memoapplication.databinding.DialogMemoPasswordBinding
import com.lion.a08_memoapplication.databinding.FragmentShowMemoAllBinding
import com.lion.a08_memoapplication.databinding.RowMemoBinding
import com.lion.a08_memoapplication.model.MemoModel
import com.lion.a08_memoapplication.repository.MemoRepository
import com.lion.a08_memoapplication.util.FragmentName
import com.lion.a08_memoapplication.util.MemoListName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ShowMemoAllFragment : Fragment() {

    lateinit var fragmentShowMemoAllBinding: FragmentShowMemoAllBinding
    lateinit var mainActivity: MainActivity

    // RecyclerView 구성을 위한 임시데이터
//    val tempData1 = Array(100){
//        "메모 제목 $it"
//    }

    // RecyclerView를 구성하기 위한 리스트
    var memoList = mutableListOf<MemoModel>()
    // 필터링 된 메모 데이터 리스트
    var filteredList = mutableListOf<MemoModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        fragmentShowMemoAllBinding = FragmentShowMemoAllBinding.inflate(layoutInflater)
        mainActivity = activity as MainActivity

        // Toolbar를 구성하는 메서드를 호출한다.
        settingToolbar()
        // RecyclerView를 구성하는 메서드를 호출한다.
        settingRecyclerView()
        // 버튼을 설정하는 메서드를 호출한다.
        settingButton()
        // 데이터를 읽어와 RecyclerView를 갱신하는 메서드를 호출한다.
        refreshRecyclerView()

        return fragmentShowMemoAllBinding.root
    }

    // Toolbar를 구성하는 메서드
    fun settingToolbar(){
        fragmentShowMemoAllBinding.apply {
            if(arguments != null){
                if(arguments?.getString("MemoName") != MemoListName.MEMO_NAME_ADDED.str) {
                    toolbarShowMemoAll.title = arguments?.getString("MemoName")
                } else {
                    toolbarShowMemoAll.title = arguments?.getString("categoryName")
                }
            } else {
                toolbarShowMemoAll.title = MemoListName.MEMO_NAME_ALL.str
            }

            // 네비게이션 아이콘을 설정하고 누를 경우 NavigationView가 나타나도록 한다.
            toolbarShowMemoAll.setNavigationIcon(R.drawable.menu_24px)
            toolbarShowMemoAll.setNavigationOnClickListener {
                mainActivity.activityMainBinding.drawerLayoutMain.open()
            }

            toolbarShowMemoAll.inflateMenu(R.menu.toolbar_main_search)
            toolbarShowMemoAll.setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.toolbar_show_searchview -> {
                        // searchView를 토글시킨다.
                        toggleSearchView()
                    }
                }
                true
            }

            // SearchView 설정
            settingSearchView()
        }
    }

    // SearchView가 나오게 안나오게 하는 메서드
    fun toggleSearchView() {
        val constraintLayout = fragmentShowMemoAllBinding.root
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        fragmentShowMemoAllBinding.searchViewMain.apply {
            // 시작 위치 (위로부터 내려올 거리)
            val startOffset = -100f

            if (visibility == View.GONE) {
                // SearchView 나타남: 슬라이드 + 페이드 인
                translationY = startOffset
                // 완전히 투명한 상태에서 시작
                alpha = 0f
                visibility = View.VISIBLE
                animate()
                    // 원래 위치로 슬라이드
                    .translationY(0f)
                    // 투명도 증가
                    .alpha(1f)
                    .setDuration(300)
                    .withEndAction {
                        // 키보드를 보여준다
                        mainActivity.showSoftInput(fragmentShowMemoAllBinding.searchViewMain)
                    }
                    .start()

                // SearchView를 VISIBLE 상태로 변경
                constraintSet.setVisibility(R.id.searchViewMain, View.VISIBLE)
            } else {
                // SearchView 사라짐: 슬라이드 + 페이드 아웃 -> 제대로 작동이 안함...
                animate()
                    // 슬라이드 위로 이동
                    .translationY(startOffset)
                    // 투명도 감소
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        // 애니메이션 후 GONE 처리
                        visibility = View.GONE
                        // SearchView 텍스트 초기화
                        setQuery("",false)
                        // 키보드를 올린다. -> 없어도 되긴하는데, 키보드 올리는 딜레이 때문에, 버그 일어나서 추가함
                        mainActivity.hideSoftInput()
                    }
                    .start()

                // SearchView를 GONE 상태로 변경
                constraintSet.setVisibility(R.id.searchViewMain, View.GONE)

            }

            // RecyclerView 위치 변경 애니메이션 적용
            val transition = ChangeBounds()
            // 애니메이션 지속 시간
            transition.duration = 300
            TransitionManager.beginDelayedTransition(constraintLayout, transition)

            // ConstraintLayout 업데이트
            constraintSet.applyTo(constraintLayout)
        }
    }

    // SearchView를 설정하는 메서드
    fun settingSearchView(){
        // SearchView가 기본적으로 아이콘 버튼 안누르게 하는거
        fragmentShowMemoAllBinding.searchViewMain.isIconifiedByDefault = false
        // fragmentShowMemoAllBinding.searchViewMain.isSubmitButtonEnabled = true
        fragmentShowMemoAllBinding.searchViewMain.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    filterRecyclerView(query)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filterRecyclerView(newText)
                    return true
                }
            }
        )
    }

    // SearchView 입력에 따라 RecyclerView를 필터링
    fun filterRecyclerView(query: String?) {
        filteredList.clear()
        if (query.isNullOrEmpty()) {
            // 검색어가 없으면 전체 데이터 사용
            filteredList.addAll(memoList)
        } else {
            filteredList.addAll(memoList.filter { it.memoTitle.contains(query, ignoreCase = true) })
        }
        fragmentShowMemoAllBinding.recyclerViewShowMemoAll.adapter?.notifyDataSetChanged()
    }



    // RecyclerView를 구성하는 메서드
    fun settingRecyclerView(){
        fragmentShowMemoAllBinding.apply {
            recyclerViewShowMemoAll.adapter = RecyclerShowMemoAdapter()
            recyclerViewShowMemoAll.layoutManager = LinearLayoutManager(mainActivity)
            val deco = MaterialDividerItemDecoration(mainActivity, MaterialDividerItemDecoration.VERTICAL)
            recyclerViewShowMemoAll.addItemDecoration(deco)

            mainActivity.fabHideAndShow(recyclerViewShowMemoAll, fabShowMemoAllAdd)
        }
    }

    // 버튼을 설정하는 메서드
    fun settingButton(){
        fragmentShowMemoAllBinding.apply {
            // fab를 누를 때
            fabShowMemoAllAdd.setOnClickListener {
                // 데이터를 담을 번들
                val dataBundle = Bundle()
                if(arguments != null) {
                    dataBundle.putString("MemoName", arguments?.getString("MemoName")!!)
                    // 만약 카테고리를 선택해서 온 것이라면.
                    if (arguments?.getString("MemoName") == MemoListName.MEMO_NAME_ADDED.str) {
                        dataBundle.putInt("categoryIdx", arguments?.getInt("categoryIdx")!!)
                        dataBundle.putString("categoryName", arguments?.getString("categoryName")!!)
                    }
                } else {
                    dataBundle.putString("MemoName", MemoListName.MEMO_NAME_ALL.str)
                }

                // AddMemoFragment가 나타나게 한다.
                mainActivity.replaceFragment(FragmentName.ADD_MEMO_FRAGMENT, true, true, dataBundle)
            }
        }
    }

    // RecyclerView의 어뎁터
    inner class RecyclerShowMemoAdapter : RecyclerView.Adapter<RecyclerShowMemoAdapter.ViewHolderMemoAdapter>(){
        // ViewHolder
        inner class ViewHolderMemoAdapter(val rowMemoBinding: RowMemoBinding) : RecyclerView.ViewHolder(rowMemoBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMemoAdapter {
            val rowMemoBinding = RowMemoBinding.inflate(layoutInflater, parent, false)
            val viewHolderMemoAdapter = ViewHolderMemoAdapter(rowMemoBinding)

            rowMemoBinding.root.setOnClickListener {
                // mainActivity.replaceFragment(FragmentName.READ_MEMO_FRAGMENT, true, true, null)
                // 항목을 눌러 메모 보는 화면으로 이동하는 처리
                showMemoData(viewHolderMemoAdapter.adapterPosition)
            }

            // 즐겨찾기 버튼 처리
            rowMemoBinding.buttonRowFavorite.setOnClickListener {
                // 사용자가 선택한 항목 번째 객체를 가져온다.
                val memoModel = filteredList[viewHolderMemoAdapter.adapterPosition]
                // 즐겨찾기 값을 반대값으로 넣어준다.
                memoModel.memoIsFavorite = !memoModel.memoIsFavorite
                // 즐겨찾기 값을 수정한다.
                CoroutineScope(Dispatchers.Main).launch {
                    val work1 = async(Dispatchers.IO){
                        MemoRepository.updateMemoFavorite(mainActivity, memoModel.memoIdx, memoModel.memoIsFavorite)
                    }
                    work1.join()

                    // 즐겨찾기 라면...
                    if(arguments?.getString("MemoName") == MemoListName.MEMO_NAME_FAVORITE.str){
                        // 현재 번째 객체를 제거한다.
                        filteredList.removeAt(viewHolderMemoAdapter.adapterPosition)
                        fragmentShowMemoAllBinding.recyclerViewShowMemoAll.adapter?.notifyItemRemoved(viewHolderMemoAdapter.adapterPosition)
                    } else {
                        val a1 = rowMemoBinding.buttonRowFavorite as MaterialButton
                        if (memoModel.memoIsFavorite) {
                            a1.setIconResource(R.drawable.star_full_24px)
                        } else {
                            a1.setIconResource(R.drawable.star_24px)
                        }
                    }
                }
            }


            return viewHolderMemoAdapter
        }

        override fun getItemCount(): Int {
            return filteredList.size
        }

        override fun onBindViewHolder(holder: ViewHolderMemoAdapter, position: Int) {
            if(filteredList[position].memoIsSecret){
                holder.rowMemoBinding.textViewRowTitle.text = "비밀 메모 입니다"
                holder.rowMemoBinding.textViewRowTitle.setTextColor(Color.LTGRAY)
            } else {
                holder.rowMemoBinding.textViewRowTitle.text = filteredList[position].memoTitle
                holder.rowMemoBinding.textViewRowTitle.setTextColor(Color.BLACK)
            }

            val a1 = holder.rowMemoBinding.buttonRowFavorite as MaterialButton
            if(filteredList[position].memoIsFavorite){
                a1.setIconResource(R.drawable.star_full_24px)
            } else {
                a1.setIconResource(R.drawable.star_24px)
            }
        }
    }

    // 데이터를 읽어와 RecyclerView를 갱신하는 메서드
    fun refreshRecyclerView(){
        memoList.clear()
        filteredList.clear()

        CoroutineScope(Dispatchers.Main).launch {
            val work1 = async(Dispatchers.IO){
                if(arguments != null){
                    when(arguments?.getString("MemoName")){
                        // 모든 메모
                        MemoListName.MEMO_NAME_ALL.str -> {
                            MemoRepository.selectMemoDataAll(mainActivity)
                        }
                        // 즐겨 찾기
                        MemoListName.MEMO_NAME_FAVORITE.str -> {
                            MemoRepository.selectMemoDataAllByFavorite(mainActivity, true)
                        }
                        // 비밀 메모
                        MemoListName.MEMO_NAME_SECRET.str -> {
                            MemoRepository.selectMemoDataAllBySecret(mainActivity, true)
                        }
                        // 사용자가 추가한 카테고리
                        else -> {
                            // 카테고리 번호
                            val categoryIdx = arguments?.getInt("categoryIdx")!!
                            MemoRepository.selectMemoDataAllByCategoryIdx(mainActivity, categoryIdx)
                        }
                    }
                } else {
                    // 전달된 카테고리 관련 데이터가 없다면 모두 가져온다.
                    MemoRepository.selectMemoDataAll(mainActivity)
                }
            }
            memoList = work1.await()
            filteredList.addAll(memoList)
            fragmentShowMemoAllBinding.recyclerViewShowMemoAll.adapter?.notifyDataSetChanged()
        }
    }

    // 항목을 눌러 메모 보는 화면으로 이동하는 처리
    fun showMemoData(position:Int){
        // 비밀 메모인지 확인한다.
        if(filteredList[position].memoIsSecret){
            val builder = MaterialAlertDialogBuilder(mainActivity)
            builder.setTitle("비밀번호 입력")

            val dialogMemoPasswordBinding = DialogMemoPasswordBinding.inflate(layoutInflater)
            builder.setView(dialogMemoPasswordBinding.root)

            builder.setNegativeButton("취소", null)
            builder.setPositiveButton("확인"){ dialogInterface: DialogInterface, i: Int ->
                // 사용자가 입력한 비밀번호를 가져온다.
                val inputPassword = dialogMemoPasswordBinding.textFieldDialogMemoPassword.editText?.text.toString()
                // 입력한 비밀번호를 제대로 입력했다면
                if(inputPassword == filteredList[position].memoPassword){
                    // 메모 번호를 전달한다.
                    val dataBundle = Bundle()
                    dataBundle.putInt("memoIdx", filteredList[position].memoIdx)
                    mainActivity.replaceFragment(FragmentName.READ_MEMO_FRAGMENT, true, true, dataBundle)
                } else {
                    val snackbar = Snackbar.make(mainActivity.activityMainBinding.root, "비밀번호를 잘못 입력하였습니다", Snackbar.LENGTH_SHORT)
                    snackbar.show()
                }
            }
            builder.show()
        } else {
            // 메모 번호를 전달한다.
            val dataBundle = Bundle()
            dataBundle.putInt("memoIdx", filteredList[position].memoIdx)
            mainActivity.replaceFragment(FragmentName.READ_MEMO_FRAGMENT, true, true, dataBundle)
        }
    }
}