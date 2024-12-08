# 검색 기능 구현

- `fragment_show_memo_all.xml` 에서 보이게 구현할 생각이다. 
- SearchView를 통해서 보이게 만들것이다.
그래서 추가했다.

```kt
    <SearchView
        android:id="@+id/searchViewMain"
        android:layout_width="0dp"
        android:layout_height="68dp"
        android:visibility="gone"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@+id/toolbarShowMemoAll" />
```

- 디자인적으로 툴바에서 버튼을 누르면 SearchView가 나오게하고. 다시 누르면 사라지게하는 기능(토글)이 좋을 것 같아서
추가했다.
- `res/menu/toolbar_main_search.xml`
```xml
 <?xml version="1.0" encoding="utf-8"?>
<menu xmlns:app="http://schemas.android.com/apk/res-auto"
xmlns:android="http://schemas.android.com/apk/res/android">

<item
android:id="@+id/toolbar_show_searchview"
android:icon="@drawable/search_24px"
android:title="검색"
app:showAsAction="ifRoom" />
</menu>
```

그리고 settingToolbar() 메서드에서 inflate 해주자
```kt
toolbarShowMemoAll.inflateMenu(R.menu.toolbar_main_search)
```

## ShowMemoAllFragment 에서 RecyclerView랑 SearchView랑 연결하기
- Visibility 속성을 이용해서 Gone 인 상태면 Visible 상태로, 아니면 Gone 상태로 만들어 줬다.
- 근데 애니메이션이 없으니 딱딱하게 텔레포트 하는 느낌이 들어서
- 애니메이션 을 넣어서 자연스럽게 해줬다
```kt
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
```

- 필터링 될 리스트를 만들어주자
```kt
    // 필터링 된 메모 데이터 리스트
    var filteredList = mutableListOf<MemoModel>()
```

- 이제 SearchView를 설정하는 메서드를 만들어본다.
```kt
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
```

- toolbar를 설정하는 메서드에 넣어줘서 적용되게 한다
```kt
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
```

- Adapter 클래스에 memoList 대신 filteredList를 적절하게 넣어줘서 수정한다.
```kt
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
```

- RecyclerView를 refresh하는 메서드도 수정한다
```kt
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
```

- 다른 항목 눌렀을 때도 적용되게 filteredList로 교체해준다.
```kt
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
```

- 이제 짜잘 짜잘한 사용자 편의성을 늘려주자
- SearchView가 등장하면, 키보드를 올려준다(보이게)
- SearchView가 사라지면, 키보드 내린다(안보이게)
- SearchView가 사라지면, 안에 있는 텍스트도 사라지게 하고 적용한다
- mainActivity에서 구현한 메서드를 활용하자

```kt
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
```



