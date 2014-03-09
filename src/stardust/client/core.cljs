(ns stardust.client.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a :refer [<!]]
            [stardust.client.events :refer [frames keyboard]]
            [stardust.client.handlers :refer [handle]]
            [stardust.client.utils :as u]))

(enable-console-print!)

(defn events-loop
  []
  (let [events (a/merge [(frames) (keyboard)])]
    (go (loop [state (stardust.models.DeathMatchScreen. (stardust.models/player 0 500 300 100 1) [])] ;; TODO
          (recur (handle state (<! events)))))))

(u/init-request-animation-frame)
(events-loop)
