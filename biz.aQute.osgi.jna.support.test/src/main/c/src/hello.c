#include <stdio.h>

static char * v = "v3";

int hello() {
        printf("Hello %s\n",v);
        fflush(stdout);
        return 0;
}

__attribute__((destructor))
static void _fini() {
    printf("[%s] [%s] [%s]\n", __FILE__, __FUNCTION__, v);

}

__attribute__((constructor))
static void _init() {
    printf("[%s] [%s] [%s]\n", __FILE__, __FUNCTION__, v);
}
