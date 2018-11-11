package io.github.vladimirmi.internetradioplayer.presentation.station

import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.github.vladimirmi.internetradioplayer.R
import io.github.vladimirmi.internetradioplayer.data.db.entity.Group
import io.github.vladimirmi.internetradioplayer.data.db.entity.Station
import io.github.vladimirmi.internetradioplayer.di.Scopes
import io.github.vladimirmi.internetradioplayer.extensions.inputMethodManager
import io.github.vladimirmi.internetradioplayer.extensions.visible
import io.github.vladimirmi.internetradioplayer.presentation.base.BaseFragment
import io.github.vladimirmi.internetradioplayer.ui.TagView
import kotlinx.android.synthetic.main.view_station_detail_info.*
import kotlinx.android.synthetic.main.view_station_info.*
import toothpick.Toothpick


/**
 * Created by Vladimir Mikhalev 18.11.2017.
 */

class StationFragment : BaseFragment<StationPresenter, StationView>(), StationView {

    override val layout = R.layout.fragment_station

    private var editTextBg: Int = 0

    override fun providePresenter(): StationPresenter {
        return Toothpick.openScopes(Scopes.ROOT_ACTIVITY, this)
                .getInstance(StationPresenter::class.java).also {
                    Toothpick.closeScope(this)
                }
    }

    override fun setupView(view: View) {
        // save default edit text background
        val typedValue = TypedValue()
        activity?.theme?.resolveAttribute(android.R.attr.editTextBackground, typedValue, true)
        editTextBg = typedValue.resourceId

        // set appropriate action on the multiline text
        titleEt.imeOptions = EditorInfo.IME_ACTION_NEXT
        titleEt.setRawInputType(InputType.TYPE_CLASS_TEXT)


        urlTv.setOnClickListener { openLink(it as TextView) }
        uriTv.setOnClickListener { openLink(it as TextView) }
    }

    override fun handleBackPressed() = presenter.onBackPressed()

    //region =============== StationView ==============

    override fun setStation(station: Station) {
        titleEt.setText(station.name)

        uriTv.text = station.uri
        uriTv.linkStyle(true)

        urlTv.text = station.url
        urlTv.linkStyle(true)

        (station.url != null).let {
            urlLabelTv.visible(it)
            urlTv.visible(it)
        }

        bitrateTv.text = getString(R.string.unit_bitrate, station.bitrate)
        (station.bitrate != null).let {
            bitrateLabelTv.visible(it)
            bitrateTv.visible(it)
        }

        sampleTv.text = getString(R.string.unit_sample_rate, station.sample)
        (station.sample != null).let {
            sampleLabelTv.visible(it)
            sampleTv.visible(it)
        }

        groupEt.setText(Group.getViewName(station.groupName, context!!))

        with(station.genres) {
            genresLabelTv.visible(isNotEmpty())
            genresFl.removeAllViews()
            forEach { genresFl.addView(TagView(context!!, it, null)) }
        }
    }

    override fun setEditMode(editMode: Boolean) {
        titleEt.setEditable(editMode)
        val groupVisible = groupEt.text.isNotBlank() || editMode
        groupLabelTv.visible(groupVisible)
        groupEt.visible(groupVisible)
        groupEt.setEditable(editMode)

        if (editMode) {
            titleEt.requestFocus()
            titleEt.setSelection(titleEt.text.length)
        } else {
            context!!.inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
        }
    }

    override fun editStation() {
        presenter.edit(constructStation())
    }

    override fun createStation() {
        presenter.create(constructStation())
    }

    override fun openRemoveDialog() {
        RemoveDialog().show(childFragmentManager, "remove_dialog")
    }

    override fun openLinkDialog(url: String) {
        LinkDialog.newInstance(url).show(childFragmentManager, "link_dialog")
    }

    override fun openCancelEditDialog() {
        CancelEditDialog().show(childFragmentManager, "cancel_edit_dialog")
    }

    override fun openAddShortcutDialog() {
        AddShortcutDialog().show(childFragmentManager, "add_shortcut_dialog")
    }

    override fun cancelEdit() {
        presenter.tryCancelEdit(constructStation())
    }

    override fun openCancelCreateDialog() {
        CancelCreateDialog().show(childFragmentManager, "cancel_create_dialog")
    }

    //endregion

    private fun constructStation(): StationInfo {
        val genres = ArrayList<String>()
        (0 until genresFl.childCount)
                .forEach {
                    val tagView = genresFl.getChildAt(it) as TagView
                    genres.add(tagView.text.toString())
                }
        return StationInfo(
                stationName = titleEt.text.toString(),
                groupName = Group.getDbName(groupEt.text.toString(), context!!),
                genres = genres)
    }

    private fun TextView.linkStyle(enable: Boolean) {
        val string = text.toString()
        val color = ContextCompat.getColor(context, R.color.blue_500)
        text = if (enable) {
            val spannable = SpannableString(string)
            spannable.setSpan(URLSpan(string), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(color), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable
        } else {
            string
        }
    }

    private fun openLink(it: TextView) {
        presenter.openLink(it.text.toString())
    }

    private fun EditText.setEditable(enable: Boolean) {
        isFocusable = enable
        isClickable = enable
        isFocusableInTouchMode = enable
        isCursorVisible = enable

        if (enable) setBackgroundResource(editTextBg)
        else setBackgroundResource(0)
    }
}
