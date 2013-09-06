/* 
 * File:   ServerSocket.h
 * Author: zoli
 *
 * Created on 2013. szeptember 5., 20:04
 */

#ifndef SERVERSOCKET_H
#define	SERVERSOCKET_H

#include "Socket.h"

#include <netinet/in.h>

class ServerSocket : private Socket {
    
    protected:
        
        struct handles {
            int socket;
            struct sockaddr_in server;
            int serverlen;
        };
    
    public:
        
        ServerSocket(uint16_t port, uint16_t maxNewConn = 10);
        
//        Socket accept();
        void close();
        bool isClosed();
        void setTimeout(int sec);
        
    protected:
        
        handles h;
        int timeout;
        
        ServerSocket();
        
        void open(uint16_t port, uint16_t maxConn);
        int tcpAccept();
        
        virtual int write(const void *buf, int num) const;
        virtual int read(void *buf, int num) const;
        
};

#endif	/* SERVERSOCKET_H */
