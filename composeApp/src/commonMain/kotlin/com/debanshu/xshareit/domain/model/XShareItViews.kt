package com.debanshu.xshareit.domain.model

sealed class XShareItViews {
    data object SelectionView : XShareItViews()

    data object SenderInitView: XShareItViews()
    data object SenderEmittingView: XShareItViews()
    data object SenderCompletedView: XShareItViews()

    data object ReceiverInitView : XShareItViews()
    data object ReceiverConsumingView: XShareItViews()
    data object ReceiverCompletedView: XShareItViews()

    data object ErrorView : XShareItViews()
}