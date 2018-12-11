package org.apache.zookeeper.quorum;

import org.apache.zookeeper.server.quorum.auth.QuorumAuthServer;
import org.apache.zookeeper.server.quorum.auth.QuorumAuthLearner;

class QuorumCnxManagerBuilder {
    private long quorumPeerId;
    // TODO probably rename to something like quorumMapView or something
    private Map<Long,QuorumPeer.QuorumServer> view;

    private QuorumAuthServer authServer;
    private QuorumAuthLearner authLearner;

    private int socketTimeout;

    private boolean listenOnAllIPs;

    private int quorumCnxnThreadsSize;

    private boolean quorumSaslAuthEnabled;

    QuorumPeer quorumPeer;

    public QuorumCnxManagerBuilder setQuorumPeer(QuorumPeer quorumPeer) {
        this.quorumPeer = quorumPeer;
        thsi.quorumPeerId = quorumPeer.getId();

        return this;
    }

    public QuorumCnxManagerBuilder setView(Map<Long,QuorumCnxManagerBuilder.QuorumServer> view) {
        this.view = view;

        return this;
    }

    public QuorumCnxManagerBuilder setAuthServer(QuorumAuthServer authServer) {
        this.authServer = authServer;

        return this;
    }

    public QuorumCnxManagerBuilder setAuthLearner(QuorumAuthLearner authLearner) {
        this.authLearner = authLearner;

        return this;
    }

    public QuorumCnxManagerBuilder setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;

        return this;
    }

    public QuorumCnxManagerBuilder setListOnAllIPs(boolean listenOnAllIPs) {
        this.listenOnAllIPs = listenOnAllIPs

        return this;
    }

    public QuorumCnxManagerBuilder setQuorumCnxnThreadsSize(int quorumCnxnThreadsSize) {
        this.quorumCnxnThreadsSize = quorumCnxnThreadsSize;

        return this;
    }

    public QuorumCnxManagerBuilder setQuorumSaslAuthEnabled(boolean quorumSaslAuthEnabled) {
        this.quorumSaslAuthEnabled = quorumSaslAuthEnabled;

        return this;
    }

    public QuorumCnxManager build() {
        return new QuorumCnxManager(quorumPeer,
                                    quorumPeerId,
                                    view,
                                    authServer,
                                    authLearner,
                                    socketTimeout,
                                    listenOnAllIPs,
                                    quorumCnxnThreadsSize,
                                    quorumSaslAuthEnabled);
    }
}
