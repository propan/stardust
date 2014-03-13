(ns stardust.client.handlers
  (:require [clojure.browser.dom :as dom]
            [cljs.core.async :refer [put!]]
            [stardust.client.draw :as d]
            [stardust.protocols :refer [Handler]]
            [stardust.models :as m :refer [ConnectionScreen DeathMatchScreen]]))

(def context (.getContext (dom/get-element "open-space") "2d"))

(defn- change-ship-state
  [state property from to]
  (update-in state [:player property] #(if (= % from) to %)))

(defn- handle-frame
  [state fps]
  (d/draw state context)
  (assoc state :fps fps))

(defn- gs-handle-keyboard
  [state event]
  (case event
    :arrow-left-down  (change-ship-state state :rotate :none :left)
    :arrow-left-up    (change-ship-state state :rotate :left :none)
    :arrow-right-down (change-ship-state state :rotate :none :right)
    :arrow-right-up   (change-ship-state state :rotate :right :none)
    :arrow-up-down    (change-ship-state state :accelerate false true)
    :arrow-up-up      (change-ship-state state :accelerate true false)
    state))

(defn- dms-handle-keyboard
  [state event]
  (put! (:out-channel state) event)
  state)

(defn handle-socket
  [{:keys [out-channel fps] :as state} [event data]]
  (case event
    :message (merge data {:fps         fps
                          :out-channel out-channel})
    :closed  (m/connection-screen out-channel)
    state))

(extend-type ConnectionScreen
  Handler
  (handle [state event]
    (let [[source data] event]
      (case source
        :frame    (handle-frame state data)
        :socket   (handle-socket state data)
        state))))

(extend-type DeathMatchScreen
  Handler
  (handle [state event]
    (let [[source data] event]
      (case source
        :frame    (handle-frame state data)
        :keyboard (dms-handle-keyboard state event)
        :socket   (handle-socket state data)
        state))))
