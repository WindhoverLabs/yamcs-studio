package org.yamcs.studio.core;

public interface ConnectionFailureListener {
    public void connectionFailure(int currentNode, int nextNode);
}
