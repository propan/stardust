(ns stardust.client.handlers
  (:require [clojure.browser.dom :as dom]
            [cljs.core.async :refer [put!]]
            [stardust.client.draw :as d]
            [stardust.models :as m :refer [ConnectionScreen DeathMatchScreen]]
            [stardust.protocols :as p :refer [Handler]]
            [stardust.tick]
            [stardust.utils :as u]))

(def context (.getContext (dom/get-element "open-space") "2d"))

(defn- change-ship-state
  [state property from to]
  (update-in state [:player property] #(if (= % from) to %)))

(defn- handle-frame
  [state between]
  (d/draw state context)
  (-> state
      (assoc :fps (u/round (/ 1000 between)))
      (p/tick (/ between 1000.0))))

(defn- dms-handle-keyboard
  [state event]
  (put! (:out-channel state) event)
  state)

(defn handle-socket
  [{:keys [out-channel fps effects] :as state} [event data]]
  (case event
    :message (merge data {:fps         fps
                          :out-channel out-channel
                          :effects     (concat effects (:effects data))
                          :score       (:score data)})
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
