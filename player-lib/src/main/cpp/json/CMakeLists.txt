aux_source_directory(. SRC_LIST)

add_library( # Sets the name of the library.
        jsoncpp

        SHARED

        ${SRC_LIST})
