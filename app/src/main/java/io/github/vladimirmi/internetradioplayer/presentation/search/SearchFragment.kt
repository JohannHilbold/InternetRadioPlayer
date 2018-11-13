package io.github.vladimirmi.internetradioplayer.presentation.search

import android.graphics.Rect
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.vladimirmi.internetradioplayer.R
import io.github.vladimirmi.internetradioplayer.di.Scopes
import io.github.vladimirmi.internetradioplayer.domain.model.Suggestion
import io.github.vladimirmi.internetradioplayer.presentation.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_search.*
import toothpick.Toothpick
import androidx.appcompat.widget.SearchView as SearchViewAndroid


/**
 * Created by Vladimir Mikhalev 12.11.2018.
 */

class SearchFragment : BaseFragment<SearchPresenter, SearchView>(), SearchView,
        SearchViewAndroid.OnQueryTextListener, View.OnFocusChangeListener {

    override val layout = R.layout.fragment_search

    private val suggestionsAdapter = SearchSuggestionsAdapter()

    override fun providePresenter(): SearchPresenter {
        return Toothpick.openScopes(Scopes.ROOT_ACTIVITY, this)
                .getInstance(SearchPresenter::class.java).also {
                    Toothpick.closeScope(this)
                }
    }

    override fun setupView(view: View) {
        suggestionsRv.layoutManager = LinearLayoutManager(context)
        suggestionsRv.adapter = suggestionsAdapter
        view.requestFocus()

        searchView.setIconifiedByDefault(false)
        searchView.setOnQueryTextListener(this)
        searchView.setOnQueryTextFocusChangeListener(this)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        presenter.search(query)
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        presenter.querySuggestions(newText)
        return true
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        adjustSuggestionsRecyclerHeight(hasFocus)
        if (hasFocus) {
            presenter.querySuggestions(searchView.query.toString())
        } else {
            suggestionsAdapter.setData(emptyList())
        }
    }

    override fun setSuggestions(list: List<Suggestion>) {
        suggestionsAdapter.setData(list)
    }

    private fun adjustSuggestionsRecyclerHeight(keyboardDisplayed: Boolean) {
        if (keyboardDisplayed) {
            Handler().postDelayed({
                val rect = Rect()
                suggestionsRv.getWindowVisibleDisplayFrame(rect)
                val xy = IntArray(2)
                suggestionsRv.getLocationInWindow(xy)

                val lp = suggestionsRv.layoutParams
                lp.height = rect.bottom - xy[1]
                suggestionsRv.layoutParams = lp
            }, 500) // wait keyboard animation
        } else {
            val lp = suggestionsRv.layoutParams
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            suggestionsRv.layoutParams = lp
        }
    }
}
