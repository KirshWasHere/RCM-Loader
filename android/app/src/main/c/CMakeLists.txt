cmake_minimum_required(VERSION 3.22.1)
project("loader")
add_library(loader SHARED smash.c)
target_link_libraries(loader log)
