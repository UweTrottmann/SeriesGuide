// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide

import com.battlelancer.seriesguide.backend.CloudSetupFragment
import com.battlelancer.seriesguide.backend.RemoveCloudAccountDialogFragment.AccountRemovedEvent
import com.battlelancer.seriesguide.backend.RemoveCloudAccountDialogFragment.CanceledEvent
import com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity
import com.battlelancer.seriesguide.billing.amazon.AmazonIapManager.AmazonIapAvailabilityEvent
import com.battlelancer.seriesguide.billing.amazon.AmazonIapManager.AmazonIapMessageEvent
import com.battlelancer.seriesguide.billing.amazon.AmazonIapManager.AmazonIapProductEvent
import com.battlelancer.seriesguide.comments.TraktCommentsFragment
import com.battlelancer.seriesguide.dataliberation.AutoBackupFragment
import com.battlelancer.seriesguide.dataliberation.DataLiberationFragment
import com.battlelancer.seriesguide.dataliberation.DataLiberationFragment.LiberationResultEvent
import com.battlelancer.seriesguide.extensions.ExtensionManager.EpisodeActionReceivedEvent
import com.battlelancer.seriesguide.extensions.ExtensionManager.MovieActionReceivedEvent
import com.battlelancer.seriesguide.lists.ListsDistillationSettings.ListsSortOrderChangedEvent
import com.battlelancer.seriesguide.lists.SgListFragment
import com.battlelancer.seriesguide.movies.MovieLocalizationDialogFragment
import com.battlelancer.seriesguide.movies.MovieLocalizationDialogFragment.ItemsLoadedEvent
import com.battlelancer.seriesguide.movies.MovieLocalizationDialogFragment.LocalizationChangedEvent
import com.battlelancer.seriesguide.movies.MoviesBaseFragment
import com.battlelancer.seriesguide.movies.MoviesDiscoverFragment
import com.battlelancer.seriesguide.movies.MoviesDistillationSettings.MoviesSortOrderChangedEvent
import com.battlelancer.seriesguide.movies.details.MovieDetailsFragment
import com.battlelancer.seriesguide.movies.details.MovieDetailsFragment.MovieChangedEvent
import com.battlelancer.seriesguide.movies.search.MoviesSearchFragment
import com.battlelancer.seriesguide.people.PersonFragment
import com.battlelancer.seriesguide.preferences.PreferencesActivityImpl.UpdateSummariesEvent
import com.battlelancer.seriesguide.preferences.SgPreferencesFragment
import com.battlelancer.seriesguide.shows.episodes.EpisodeDetailsFragment
import com.battlelancer.seriesguide.shows.episodes.EpisodesActivity
import com.battlelancer.seriesguide.shows.history.ShowsHistoryFragment
import com.battlelancer.seriesguide.shows.overview.OverviewActivityImpl
import com.battlelancer.seriesguide.shows.overview.OverviewFragment
import com.battlelancer.seriesguide.shows.overview.SeasonsFragment
import com.battlelancer.seriesguide.shows.overview.ShowFragment
import com.battlelancer.seriesguide.shows.search.EpisodeSearchFragment
import com.battlelancer.seriesguide.shows.search.SearchActivityImpl.SearchQueryEvent
import com.battlelancer.seriesguide.shows.search.ShowSearchFragment
import com.battlelancer.seriesguide.shows.search.discover.AddShowDialogFragment
import com.battlelancer.seriesguide.shows.search.discover.BaseAddShowsFragment
import com.battlelancer.seriesguide.shows.search.discover.OnAddingShowEvent
import com.battlelancer.seriesguide.shows.search.discover.ShowsDiscoverFragment
import com.battlelancer.seriesguide.shows.search.discover.ShowsDiscoverPagingFragment
import com.battlelancer.seriesguide.shows.search.discover.TraktAddFragment
import com.battlelancer.seriesguide.shows.tools.AddShowTask.OnShowAddedEvent
import com.battlelancer.seriesguide.shows.tools.ShowTools2.OnRemovingShowEvent
import com.battlelancer.seriesguide.shows.tools.ShowTools2.OnShowRemovedEvent
import com.battlelancer.seriesguide.traktapi.GenericCheckInDialogFragment
import com.battlelancer.seriesguide.traktapi.GenericCheckInDialogFragment.CheckInDialogDismissedEvent
import com.battlelancer.seriesguide.traktapi.QuickCheckInActivity
import com.battlelancer.seriesguide.traktapi.TraktTask.TraktActionCompleteEvent
import com.battlelancer.seriesguide.traktapi.TraktTask.TraktCheckInBlockedEvent
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceActiveEvent
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceCompletedEvent
import com.battlelancer.seriesguide.ui.dialogs.L10nDialogFragment.LanguageChangedEvent
import com.battlelancer.seriesguide.util.DBUtils.DatabaseErrorEvent
import com.battlelancer.seriesguide.util.TabClickEvent
import com.battlelancer.seriesguide.util.tasks.BaseShowActionTask.ShowChangedEvent
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.meta.SimpleSubscriberInfo
import org.greenrobot.eventbus.meta.SubscriberInfo
import org.greenrobot.eventbus.meta.SubscriberInfoIndex
import org.greenrobot.eventbus.meta.SubscriberMethodInfo

/**
 * This is based on the class generated by EventBus, added to avoid running kapt just for it.
 *
 * Make sure to update class and method names below if [org.greenrobot.eventbus.Subscribe]
 * annotated methods change.
 *
 * EventBus should no longer be used in new code.
 */
class SgEventBusIndex : SubscriberInfoIndex {

    override fun getSubscriberInfo(subscriberClass: Class<*>?): SubscriberInfo? {
        return SUBSCRIBER_INDEX[subscriberClass]
    }

    companion object {
        private val SUBSCRIBER_INDEX: MutableMap<Class<*>, SubscriberInfo> = HashMap()

        init {

            putIndex(
                SimpleSubscriberInfo(
                    MoviesDiscoverFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventLanguageChanged",
                            LocalizationChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    GenericCheckInDialogFragment::class.java,
                    true, arrayOf(
                        SubscriberMethodInfo(
                            "onEvent",
                            TraktActionCompleteEvent::class.java
                        ),
                        SubscriberMethodInfo(
                            "onEvent",
                            TraktCheckInBlockedEvent::class.java
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    MovieDetailsFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            MovieActionReceivedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEvent",
                            MovieChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventEpisodeTask",
                            ServiceActiveEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventEpisodeTask",
                            ServiceCompletedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "handleLanguageEvent",
                            LocalizationChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    CloudSetupFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            CanceledEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            AccountRemovedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    MovieLocalizationDialogFragment::class.java,
                    true, arrayOf(
                        SubscriberMethodInfo(
                            "onEventItemsLoaded",
                            ItemsLoadedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    OverviewFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            EpisodeActionReceivedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventEpisodeTask",
                            ServiceActiveEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventEpisodeTask",
                            ServiceCompletedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    AddShowDialogFragment::class.java,
                    true, arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            LanguageChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    SeasonsFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEvent",
                            ServiceCompletedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    ShowsDiscoverPagingFragment::class.java,
                    true, arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            LanguageChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    TraktAddFragment::class.java,
                    true, arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            ShowChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEvent",
                            OnAddingShowEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEvent",
                            OnShowAddedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEvent",
                            OnShowRemovedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    ShowsDiscoverFragment::class.java,
                    true, arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            LanguageChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    MoviesBaseFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            MoviesSortOrderChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    BaseMessageActivity::class.java,
                    true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onServiceActiveEvent",
                            ServiceActiveEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onServiceCompletedEvent",
                            ServiceCompletedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    EpisodeDetailsFragment::class.java,
                    true, arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            EpisodeActionReceivedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventEpisodeTask",
                            ServiceActiveEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventEpisodeTask",
                            ServiceCompletedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    OverviewActivityImpl::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            OnRemovingShowEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    AutoBackupFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEvent",
                            LiberationResultEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    ShowFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            LanguageChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    TraktCommentsFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            TraktActionCompleteEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    QuickCheckInActivity::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEvent",
                            CheckInDialogDismissedEvent::class.java
                        ),
                        SubscriberMethodInfo(
                            "onEvent",
                            TraktActionCompleteEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    ShowsHistoryFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventEpisodeTask",
                            ServiceCompletedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    EpisodeSearchFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            SearchQueryEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventTabClick",
                            TabClickEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    BaseActivity::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEvent",
                            OnShowAddedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEvent",
                            TraktActionCompleteEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            DatabaseErrorEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    EpisodesActivity::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEvent",
                            ServiceCompletedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    MoviesSearchFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventLanguageChanged",
                            LocalizationChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    PersonFragment::class.java,
                    true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            LanguageChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    DataLiberationFragment::class.java,
                    true, arrayOf(
                        SubscriberMethodInfo(
                            "onEvent",
                            LiberationResultEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    AmazonBillingActivity::class.java,
                    true, arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            AmazonIapMessageEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            AmazonIapAvailabilityEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            AmazonIapProductEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    ShowSearchFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            SearchQueryEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventTabClick",
                            TabClickEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    BaseAddShowsFragment::class.java,
                    true, arrayOf(
                        SubscriberMethodInfo(
                            "onEventAddingShow",
                            OnAddingShowEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventShowAdded",
                            OnShowAddedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEventShowRemoved",
                            OnShowRemovedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    SgListFragment::class.java,
                    true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEventMainThread",
                            ListsSortOrderChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )

            putIndex(
                SimpleSubscriberInfo(
                    SgPreferencesFragment::class.java, true,
                    arrayOf(
                        SubscriberMethodInfo(
                            "onEvent",
                            UpdateSummariesEvent::class.java,
                            ThreadMode.MAIN
                        ),
                        SubscriberMethodInfo(
                            "onEvent",
                            LanguageChangedEvent::class.java,
                            ThreadMode.MAIN
                        ),
                    )
                )
            )
        }

        private fun putIndex(info: SubscriberInfo) {
            SUBSCRIBER_INDEX[info.subscriberClass] = info
        }
    }
}
