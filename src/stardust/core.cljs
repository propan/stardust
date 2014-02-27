(ns stardust.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a :refer [<! filter< map<]]
            [clojure.browser.dom :as dom]
            [goog.events :as events]
            [stardust.events :refer [frames listen]]
            [stardust.utils :as u]))

(enable-console-print!)

(def canvas (dom/get-element "open-space"))
(def ctx (.getContext canvas "2d"))

(defn events-loop
  []
  (let [events (a/merge [(listen js/document events/EventType.KEYDOWN)
                         (frames)])]
    (go
     (loop [state {}]
       (let [e (<! events)]
         (recur state))))))

(u/init-request-animation-frame)
(events-loop)
