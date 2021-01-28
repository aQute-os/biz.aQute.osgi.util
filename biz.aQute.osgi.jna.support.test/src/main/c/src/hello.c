#include <stdio.h>
#include <stdlib.h>

static char * v = "v3";
        static char * h = "Hello";
        static char * g = "Goodbye";
        static char * s = "Sigh";
        
        int hello() {
                printf("Hello %s\n",v);
                fflush(stdout);
                return 0;
        }
        
        typedef struct {
            char * text;
        } Foo ;
        
        Foo * create() {
            Foo * f = malloc(sizeof(Foo));
            f->text = h;
            printf("create %p %p\n",(void*)f, (void*) f->text);
            fflush(stdout);
            return f;
        }
        
        void close(Foo * f){
            f->text = g;
            printf("close %p %p\n",(void*)f, (void*) f->text);
            fflush(stdout);
            free(f);
        }
        
        void fill(Foo * f) {
            printf("fill %p %p\n",(void*)f, (void*) f->text);
            f->text = s;
        }

__attribute__((destructor))
static void _fini() {
    printf("[%s] [%s] [%s]\n", __FILE__, __FUNCTION__, v);

}

__attribute__((constructor))
static void _init() {
    printf("[%s] [%s] [%s]\n", __FILE__, __FUNCTION__, v);
}
