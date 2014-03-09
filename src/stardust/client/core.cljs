(ns stardust.client.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a :refer [<!]]
            [stardust.client.events :refer [frames keyboard websocket]]
            [stardust.protocols :refer [handle]]
            [stardust.client.handlers]
            [stardust.client.utils :as u]))

(enable-console-print!)

(defn events-loop
  []
  (let [{:keys [in out]} (websocket "ws://localhost:8080/")
        events           (a/merge [(frames) (keyboard) in])]
    (go (loop [state (stardust.models.DeathMatchScreen. out (stardust.models/player 0 500 300 100 1) [])] ;; TODO
          (recur (handle state (<! events)))))))

(u/init-request-animation-frame)
(events-loop)
