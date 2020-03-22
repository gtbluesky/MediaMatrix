//
// Created by gtbluesky on 2019-07-17.
//

#ifndef NATIVEOPENGL_LOOPER_H
#define NATIVEOPENGL_LOOPER_H

#include <pthread.h>
#include <sys/types.h>
#include <semaphore.h>

struct LooperMessage {
    int what;
    int arg1;
    int arg2;
    void *obj;
    LooperMessage *next;
    bool quit;
};

class Looper {

public:
    Looper();
    Looper&operator=(const Looper&) = delete;
    Looper(Looper&) = delete;
    virtual ~Looper();

    void postMessage(int what, bool flush = false);
    void postMessage(int what, void *obj, bool flush = false);
    void postMessage(int what, int arg1, int arg2, bool flush = false);
    void postMessage(int what, int arg1, int arg2, void *obj, bool flush = false);
    void quit();
    virtual void handleMessage(LooperMessage *msg);

private:
    void addMessage(LooperMessage *msg, bool flush);
    static void *trampoline(void *p);
    void loop(void);

    LooperMessage *head = nullptr;
    pthread_t worker;
    sem_t headwriteprotect;
    sem_t headdataavailable;
    bool running;

};


#endif //NATIVEOPENGL_LOOPER_H
