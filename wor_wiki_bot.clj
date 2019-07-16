(ns wor-wiki-bot
  (:require [aero.core :as aero]
            [clojure.java.jdbc :as jdbc]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import [org.mariadb.jdbc Driver]))

#_(Class/forName "org.mariadb.jdbc.Driver")

(def activity-query
  (str "select * from activities "
       "where created_at >= date_sub(now(), interval 1 minute);"))

(def config (aero/read-config "config.edn"))
(def webhook (:webhook/url config))
(def db {:connection-uri (:database/uri config)})
(def website (:website/url config))

(def entity_type->table {"BookStack\\Book" :books
                         "BookStack\\Page" :pages
                         "BookStack\\Chapter" :chapters
                         "BookStack\\Bookshelf" :bookshelves})

(def entity_type->noun {"BookStack\\Book" "book"
                        "BookStack\\Page" "pages"
                        "BookStack\\Chapter" "chapters"
                        "BookStack\\Bookshelf" "bookshelves"})

;; Hydration is a bad idea, but it's necessary for the entity, and much more
;; convenient data structure wise than dealing with column name collisions, or
;; trying to list and rename all the columns.
(defn hydrate-entity
  [db activity]
  (assoc activity :activity/entity
         (jdbc/get-by-id db
                         (entity_type->table (:entity_type activity))
                         (:entity_id activity))))

(defn hydrate-user
  [db activity]
  (assoc activity :activity/user (jdbc/get-by-id db :users (:user_id activity))))

(defn hydrate-book
  [db activity]
  (assoc activity :activity/book (jdbc/get-by-id db :books (:book_id activity))))

(defn activity-hydrate
  [db activity]
  (->> activity
       (hydrate-user db)
       (hydrate-entity db)
       (hydrate-book db)))

(defn url-for-user
  [website user]
  (str website "/user/" (:id user)))

(defn entity-description
  [entity]
  ;; Assume page
  ;; TODO: fix so this breaks on words.
  (str (apply str (take 200 (:text entity))) "..."))

(defn slugize
  [s]
  (-> s
      string/lower-case
      (string/replace #" " "-")))

(defn url-for
  [website act]
  ;; Assume page
  (str website
       "/books/"
       (slugize (-> act :activity/book :name))
       "/page/"
       (slugize (-> act :activity/entity :name))))

(def activity-keys #{"book_create"
                     "page_create"
                     "chapter_create"
                     "page_update"
                     "bookshelf_create"
                     "commented_on"
                     "chapter_update"
                     "book_sort"
                     "page_delete"
                     "bookshelf_delete"
                     "book_update"
                     "page_move"
                     "chapter_delete"
                     "page_restore"})

(defn fix-verb
  [v]
  (case v
    "sort" "sorted"
    ;; Default
    (str v "d")))

#_(map key->sentence activity-keys)

(defn activity->sentence
  [{:keys [key] :as act}]
  (if (= key "commented_on")
    (str "Commented on a " (entity_type->noun (:entity_type act)))
    (let [[_ noun verb] (re-find #"(.+)_(.+)" key)
          verb (fix-verb verb)]
      (str verb " a " noun))))

(defn activity->message
  [website act]
  (let [username (-> act :activity/user :name)
        action (activity->sentence act)
        user-link (url-for-user website (:activity/user act))]
    {:content (format "[%s](%s) %s" username user-link action)
     :embeds [{:title (-> act :activity/entity :name)
               :description (entity-description (:activity/entity act))
               :url (url-for website act)}]}))

(defn do-webhook!
  [url contents]
  (client/post
   url
   {:form-params (merge contents
                        {:username "WoR Wiki Bot"})
    :content-type :json}))

(defn block-until-second
  []
  (when (not= 0 (.getSecond (java.time.OffsetDateTime/now)))
    (Thread/sleep 1000)
    (recur)))

(defn push-activities
  []
  (let [acts (->> (jdbc/query db activity-query)
                  (map (partial activity-hydrate db)))]
    (doseq [act acts]
      (try
        (do-webhook! webhook (activity->message website act))
        (catch Exception e
          (log/error e "Error when pushing webhook"))))
    (count acts)))

(defn -main
  [& args]
  (loop []
    (block-until-second)
    (log/warn "Waking at " (java.time.OffsetDateTime/now))
    (log/info "Encountered " (push-activities) " activities")
    (Thread/sleep 1000)
    (recur)))
