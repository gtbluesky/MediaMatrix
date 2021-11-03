//
// Created by 甘涛 on 2018/9/30.
//

#ifndef VIDEOEDITSDK_SECURITYUTIL_H
#define VIDEOEDITSDK_SECURITYUTIL_H

#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/buffer.h>
#include <openssl/sha.h>
#include <openssl/md5.h>

class SecurityUtil {

public:
    static char *base64Encode(const char *buffer, bool newLine);

    static char *base64Decode(const char *input, bool newLine);

    static char *md5Encode(const char *input);

    static char *sha1Encode(const char *input);

    static char *sha224Encode(const char *input);

    static char *sha256Encode(const char *input);

    static char *sha384Encode(const char *input);

    static char *sha512Encode(const char *input);
};


#endif //VIDEOEDITSDK_SECURITYUTIL_H
