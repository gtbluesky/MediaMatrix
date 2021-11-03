//
// Created by 甘涛 on 2018/9/30.
//
#include "SecurityUtil.h"

char *SecurityUtil::base64Encode(const char *buffer, bool newLine) {
    BIO *bmem = NULL;
    BIO *b64 = NULL;
    BUF_MEM *bptr;

    b64 = BIO_new(BIO_f_base64());
    if (!newLine) {
        BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
    }
    bmem = BIO_new(BIO_s_mem());
    b64 = BIO_push(b64, bmem);
    BIO_write(b64, buffer, strlen(buffer));
    BIO_flush(b64);
    BIO_get_mem_ptr(b64, &bptr);
    BIO_set_close(b64, BIO_NOCLOSE);

    char *buff = (char *) malloc(bptr->length + 1);
    memcpy(buff, bptr->data, bptr->length);
    buff[bptr->length] = 0;
    BIO_free_all(b64);

    return buff;
}

char *SecurityUtil::base64Decode(const char *input, bool newLine) {
    BIO *b64 = NULL;
    BIO *bmem = NULL;
    int length = strlen(input);
    char *buffer = (char *) malloc(length);
    memset(buffer, 0, length);
    b64 = BIO_new(BIO_f_base64());
    if (!newLine) {
        BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
    }
    bmem = BIO_new_mem_buf(input, length);
    bmem = BIO_push(b64, bmem);
    BIO_read(bmem, buffer, length);
    BIO_free_all(bmem);

    return buffer;
}

char *SecurityUtil::md5Encode(const char *input) {
    MD5_CTX ctx;
    unsigned char digest[MD5_DIGEST_LENGTH];
    char *mdString = (char *) malloc(MD5_DIGEST_LENGTH * 2 + 1);
    memset(digest, 0, sizeof(digest));
    MD5_Init(&ctx);
    MD5_Update(&ctx, input, strlen(input));
    MD5_Final(digest, &ctx);
    OPENSSL_cleanse(&ctx, sizeof(ctx));
    for (int i = 0; i < MD5_DIGEST_LENGTH; i++) {
        sprintf(&mdString[i * 2], "%02x", digest[i]);
    }
    return mdString;
}

char *SecurityUtil::sha1Encode(const char *input) {
    SHA_CTX ctx;
    unsigned char digest[SHA_DIGEST_LENGTH];
    char *mdString = (char *) malloc(SHA_DIGEST_LENGTH * 2 + 1);
    SHA1((unsigned char *) input, strlen(input), digest);
    SHA1_Init(&ctx);
    SHA1_Update(&ctx, input, strlen(input));
    SHA1_Final(digest, &ctx);
    OPENSSL_cleanse(&ctx, sizeof(ctx));
    for (int i = 0; i < SHA_DIGEST_LENGTH; i++) {
        sprintf(&mdString[i * 2], "%02x", digest[i]);
    }
    return mdString;
}

char *SecurityUtil::sha224Encode(const char *input) {
    SHA256_CTX ctx;
    unsigned char digest[SHA224_DIGEST_LENGTH];
    char *mdString = (char *) malloc(SHA224_DIGEST_LENGTH * 2 + 1);
    SHA256((unsigned char *) input, strlen(input), digest);
    SHA256_Init(&ctx);
    SHA256_Update(&ctx, input, strlen(input));
    SHA256_Final(digest, &ctx);
    OPENSSL_cleanse(&ctx, sizeof(ctx));
    for (int i = 0; i < SHA224_DIGEST_LENGTH; i++) {
        sprintf(&mdString[i * 2], "%02x", digest[i]);
    }
    return mdString;
}

char *SecurityUtil::sha256Encode(const char *input) {
    SHA256_CTX ctx;
    unsigned char digest[SHA256_DIGEST_LENGTH];
    char *mdString = (char *) malloc(SHA256_DIGEST_LENGTH * 2 + 1);
    SHA256((unsigned char *) input, strlen(input), digest);
    SHA256_Init(&ctx);
    SHA256_Update(&ctx, input, strlen(input));
    SHA256_Final(digest, &ctx);
    OPENSSL_cleanse(&ctx, sizeof(ctx));
    for (int i = 0; i < SHA256_DIGEST_LENGTH; i++) {
        sprintf(&mdString[i * 2], "%02x", digest[i]);
    }
    return mdString;
}

char *SecurityUtil::sha384Encode(const char *input) {
    SHA512_CTX ctx;
    unsigned char digest[SHA384_DIGEST_LENGTH];
    char *mdString = (char *) malloc(SHA384_DIGEST_LENGTH * 2 + 1);
    SHA384((unsigned char *) input, strlen(input), digest);
    SHA384_Init(&ctx);
    SHA384_Update(&ctx, input, strlen(input));
    SHA384_Final(digest, &ctx);
    OPENSSL_cleanse(&ctx, sizeof(ctx));
    for (int i = 0; i < SHA384_DIGEST_LENGTH; i++) {
        sprintf(&mdString[i * 2], "%02x", digest[i]);
    }
    return mdString;
}

char *SecurityUtil::sha512Encode(const char *input) {
    SHA512_CTX ctx;
    unsigned char digest[SHA512_DIGEST_LENGTH];
    char *mdString = (char *) malloc(SHA512_DIGEST_LENGTH * 2 + 1);
    SHA512((unsigned char *) input, strlen(input), digest);
    SHA512_Init(&ctx);
    SHA512_Update(&ctx, input, strlen(input));
    SHA512_Final(digest, &ctx);
    OPENSSL_cleanse(&ctx, sizeof(ctx));
    for (int i = 0; i < SHA512_DIGEST_LENGTH; i++) {
        sprintf(&mdString[i * 2], "%02x", digest[i]);
    }
    return mdString;
}
