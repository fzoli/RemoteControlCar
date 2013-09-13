/* 
 * File:   SSLHandler.h
 * Author: zoli
 *
 * Created on 2013. szeptember 7., 8:13
 */

#ifndef SSLHANDLER_H
#define	SSLHANDLER_H

#include "SSLSocketter.h"

#include <vector>

class SSLHandler : public SSLSocketter {
    
public:
    
    SSLHandler(SSLSocket* socket);
    
    SSLSocket* getSocket();
    int getDeviceId();
    int getConnectionId();
    virtual void onException(std::exception &ex) = 0;
    
    static std::vector<SSLSocketter*> PROCS;
    
protected:
    
    virtual SSLSocketter* createProcess() = 0;
    virtual void init();
    virtual void onProcessNull() = 0;
    static bool equals(SSLHandler* h1, SSLHandler* h2);
    
private:
    
    SSLSocket* socket;
    int deviceId, connectionId;
    static std::string VAL_OK;
    static pthread_mutex_t mutexProcs, mutexInit;
    
    void runInit();
    void readStatus();
    
    static void addProcess(SSLSocketter* p);
    static void removeProcess(SSLSocketter* p);
    static void* run(void*);
    
};

#endif	/* SSLHANDLER_H */

