//
// Created by gtbluesky on 18-10-5.
//

#include "PcmBean.h"

PcmBean::PcmBean(SAMPLETYPE *buffer, int size) {
    this->buffer = (char *) malloc(size);
    this->buffer_size = size;
    memcpy(this->buffer, buffer, size);
}

PcmBean::~PcmBean() {
    free(buffer);
    buffer = NULL;
}
