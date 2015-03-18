(ns ^:figwheel-always re-frame-js.core
    (:require-macros [reagent.ratom :as ra])
    (:require [reagent.core :as r]
              [re-frame.core :as rf]))

(enable-console-print!)

(def initial-state  {:graph {:nodes [{:data {:name "Node A" :id "a"}}
                                     {:data {:name "Node B" :id "b"}}
                                     {:data {:name "Node C" :id "c"}}]
                             :edges [{:data {:id "ab" :source "a" :target "b"}}]}
                     :something-else [1 2 3]})


(defn sub->handler [handler sub & args]
  (let [x (rf/subscribe sub)]
    (ra/run! (rf/dispatch (into [handler @x] args)))))

(rf/register-handler
 :re-render-graph
 (fn [db [_ {n :nodes e :edges :as g} elem]]
   (when g
     (let [config {:container elem
                   :elements {:nodes n
                              :edges e}
                   :layout {:name :concentric
                            :padding 100
                            }}]
       (js/cytoscape (clj->js config))))
   db))

(rf/register-sub
 :graph
 (fn [db _]
   (ra/reaction (get-in @db [:graph]))))


(defn graph []
  (r/create-class
   {:component-did-mount
    (fn [c]
      (let [elem (.getDOMNode c)]
        (sub->handler :re-render-graph [:graph] elem)))
    :reagent-render (fn []  [:div {:id "cy"}])}))

(defn container []
  [:div
   [:input {:type :button :value "Add node" :on-click #(rf/dispatch [:add-node])}]
   [:input {:type :button :value "Add edge" :on-click #(rf/dispatch [:add-edge])}]
   [graph]])


(rf/register-handler
 :init
 (fn [_ _] initial-state))

(rf/register-handler
 :add-node
 (fn [db _]
   (let [n (str (rand-int 10000))
         ns (get-in db [:graph :nodes])]
     (assoc-in db [:graph :nodes] (conj ns {:data {:id n :name n}})))))

(rf/register-handler
 :add-edge
 (fn [db _]
   (let [n (str (rand-int 10000))
         ns (get-in db [:graph :nodes])
         {{f :id} :data} (rand-nth ns)
         {{t :id} :data} (rand-nth ns)
         es (get-in db [:graph :edges]) ]
     (assoc-in db [:graph :edges] (conj es {:data {:id n :source f :target t}})))))


(defn ^:extern run[]
  (rf/dispatch [:init])
  (r/render [container]
            (js/document.getElementById "app")))


(run)
