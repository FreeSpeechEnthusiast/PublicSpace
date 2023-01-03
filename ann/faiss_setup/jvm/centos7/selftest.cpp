#include <iostream>

extern int get_num_gpus();

int main () { 
    std::cout << "I'm alive " << get_num_gpus();
    return 0;
}