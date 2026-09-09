package com.eurovision.analytics.listening;

import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.Provider;

import java.util.List;

/** One implementation per provider, driven by that provider's own {@code *SyncScheduler}. */
public interface RecentPlayProviderClient {

    Provider provider();

    List<RecentPlay> fetchRecentPlays(ConnectedAccount account);
}
