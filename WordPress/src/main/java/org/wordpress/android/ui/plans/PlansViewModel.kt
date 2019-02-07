package org.wordpress.android.ui.plans

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PlanOffersAction.FETCH_PLAN_OFFERS
import org.wordpress.android.fluxc.generated.PlanOffersActionBuilder
import org.wordpress.android.fluxc.model.plans.PlanOffersModel
import org.wordpress.android.fluxc.store.PlanOffersStore
import org.wordpress.android.fluxc.store.PlanOffersStore.OnPlanOffersFetched
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class PlansViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused")
    private var plansStore: PlanOffersStore,
    @param:Named(UI_SCOPE) private val uiScope: CoroutineScope
) : ViewModel() {
    enum class PlansListStatus {
        DONE,
        ERROR,
        FETCHING
    }

    private val _listStatus = MutableLiveData<PlansListStatus>()
    val listStatus: LiveData<PlansListStatus>
        get() = _listStatus

    private val _plans = MutableLiveData<List<PlanOffersModel>>()
    val plans: LiveData<List<PlanOffersModel>>
        get() = _plans

    private val _showDialog = SingleLiveEvent<PlanOffersModel>()
    val showDialog: LiveData<PlanOffersModel>
        get() = _showDialog

    private var isStarted = false

    init {
        dispatcher.register(this)
    }

    fun create() {
        if (isStarted) {
            return
        }
        fetchPlans()
        isStarted = true
    }

    private fun fetchPlans() {
        _listStatus.value = PlansListStatus.FETCHING
        uiScope.launch {
            dispatcher.dispatch(PlanOffersActionBuilder.generateNoPayloadAction(FETCH_PLAN_OFFERS))
        }
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    fun onItemClicked(item: PlanOffersModel) {
        _showDialog.value = item
    }

    fun onPullToRefresh() {
        fetchPlans()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onPlanOffersFetched(event: OnPlanOffersFetched) {
        if (event.isError) {
            _listStatus.value = PlansListStatus.ERROR
            AppLog.e(T.API, "An error occurred while fetching plans")
        } else {
            _listStatus.value = PlansListStatus.DONE
        }
        _plans.value = event.planOffers // in case of PlansListStatus.ERROR this might contain cached plans
    }
}
