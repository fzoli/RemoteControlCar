/* 
 * File:   DisconnectProcess.cpp
 * Author: zoli
 * 
 * Created on 2013. szeptember 8., 23:46
 */

#include "DisconnectProcess.h"
#include "SocketException.h"

#include <iostream>

class DisconnectTimer : public Timer {
    
    private:
        
        DisconnectProcess* dp;
        std::exception* lastError;
        
    public:
        
        DisconnectTimer(DisconnectProcess* proc, unsigned int timeout2) : Timer(timeout2, 0, true) {
            dp = proc;
            lastError = NULL;
        }
        
        void start(std::exception* ex) {
            lastError = ex;
            Timer::start();
        }
        
        void tick() {
            dp->callOnDisconnect(lastError);
        }
        
};

DisconnectProcess::DisconnectProcess(SSLHandler* handler, unsigned int timeout1Sec, unsigned int timeout2Sec, unsigned int waitTimeMs) : SSLProcess(handler) {
    disconnected = false;
    timer = new DisconnectTimer(this, timeout2Sec);
    getSocket()->setTimeout(timeout1Sec);
    this->waitTime = waitTimeMs;
}

DisconnectProcess::~DisconnectProcess() {
    delete timer;
}

void DisconnectProcess::onConnect() {
    std::cout << "connected\n";
}

void DisconnectProcess::beforeAnswer() {
    ;
}

void DisconnectProcess::afterAnswer() {
    std::cout << "answer\n";
}

void DisconnectProcess::afterTimeout() {
    std::cout << "timeout over\n";
}

void DisconnectProcess::onTimeout(std::exception* ex) {
    std::cout << "timeout\n";
}

void DisconnectProcess::onDisconnect(std::exception* ex) {
    std::cout << "disconnected\n";
}

void DisconnectProcess::setTimeoutActive(bool b, std::exception* ex) {
    DisconnectTimer* dt = (DisconnectTimer*) timer;
    if (b) dt->start(ex);
    else dt->stop();
}

void DisconnectProcess::callOnDisconnect(std::exception* ex) {
    if (!disconnected) {
        disconnected = true;
        onDisconnect(ex);
    }
}

void DisconnectProcess::callOnTimeout(std::exception* ex) {
    timeout = true;
    if (!disconnected) onTimeout(ex);
}

void DisconnectProcess::callAfterAnswer() {
    if (timeout) {
        timeout = false;
        afterTimeout();
    }
    afterAnswer();
}

void DisconnectProcess::run() {
    onConnect();
    try {
        while(!getSocket()->isClosed() && !disconnected) {
            try {
                getSocket()->write(1);
                beforeAnswer();
                if (getSocket()->read() != -1) {
                    setTimeoutActive(false, NULL);
                    callAfterAnswer();
                }
                else {
                    throw std::runtime_error("socket closed");
                }
            }
            catch (SocketException &ex) {
                switch (ex.cause()) {
                    case SocketException::read:
                        setTimeoutActive(true, &ex);
                        callOnTimeout(&ex);
                        break;
                    case SocketException::write:
                        throw std::runtime_error("socket closed");
                    default:
                        throw;
                }
            }
            usleep(waitTime);
        }
    }
    catch (std::exception &ex) {
        callOnDisconnect(&ex);
    }
}