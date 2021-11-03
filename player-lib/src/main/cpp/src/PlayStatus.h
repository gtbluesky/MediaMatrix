//
// Created by gtbluesky on 18-9-18.
//

#ifndef MUSICPLAYER_PLAYSTATUS_H
#define MUSICPLAYER_PLAYSTATUS_H


class PlayStatus {

public:
    bool is_exited = false;
    bool is_loading = true;
    bool is_seeking = false;

public:
    PlayStatus();
    ~PlayStatus();
};


#endif //MUSICPLAYER_PLAYSTATUS_H
