package io.github.vladimirmi.radius.model.entity

import java.lang.IllegalStateException

/**
 * Created by Vladimir Mikhalev 11.10.2017.
 */

class GroupingMedia(private val mediaList: ArrayList<Media>)
    : MutableList<Media> by mediaList, GroupedList<Media> {

    private val groups = HashMap<String, ArrayList<Int>>()
    private val mappings = ArrayList<GroupMapping>()

    init {
        mediaList.forEachIndexed { index, media ->
            addToMappings(media, index)
        }
    }

    override fun isGroupTitle(position: Int): Boolean {
        return getGroupMapping(position).isGroupTitle
    }

    override fun getGroupTitle(position: Int): String {
        return getGroupMapping(position).group
    }

    override fun getGroupItem(position: Int): Media {
        val groupMapping = getGroupMapping(position)
        if (groupMapping.index == null) throw IllegalStateException("Should call getGroupTitle()")
        val group = groups[groupMapping.group] ?: throw IllegalStateException("Can not find group")
        return get(group[groupMapping.index])
    }

    override fun hideGroup(group: String) {
        setGroupVisible(group, false)
    }

    override fun showGroup(group: String) {
        setGroupVisible(group, true)
    }

    override fun isGroupVisible(group: String): Boolean {
        return mappings.find { it.group == group && !it.isGroupTitle }?.visible ?: false
    }

    override fun groupedSize(): Int {
        return mappings.count { it.visible }
    }

    private fun setGroupVisible(group: String, visible: Boolean) {
        mappings.forEach {
            if (it.group == group && !it.isGroupTitle) {
                it.visible = visible
            }
        }
    }

    private fun getGroupMapping(position: Int): GroupMapping {
        val hided = (0..position).count { !mappings[it].visible }
        return mappings[position + hided]
    }

    private fun addToMappings(media: Media, index: Int) {
        val group = media.dirName
        val list = groups.getOrPut(group) { ArrayList() }
        if (list.isEmpty()) mappings.add(GroupMapping(group))
        mappings.add(GroupMapping(group, list.size))
        list.add(index)
    }

    override fun add(element: Media): Boolean {
        addToMappings(element, mediaList.size)
        return mediaList.add(element)
    }
}
