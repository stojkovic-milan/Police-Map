package com.example.policemap.data

data class FilterSortOptions(
    var sortBy: SortOptions,
    var filterOptions: FilterOptions
)

enum class SortOptions {
    TimeA, TimeD, RatingA, RatingD
}
