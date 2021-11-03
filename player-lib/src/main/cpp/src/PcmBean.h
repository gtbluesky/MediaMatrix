//
// Created by gtbluesky on 18-10-5.
//

#ifndef MUSICPLAYER_PCMBEAN_H
#define MUSICPLAYER_PCMBEAN_H

#include <SoundTouch.h>

using namespace soundtouch;

class PcmBean {
public:
    char *buffer;
    int buffer_size;

public:
    PcmBean(SAMPLETYPE *buffer, int size);

    ~PcmBean();

};


#endif //MUSICPLAYER_PCMBEAN_H
