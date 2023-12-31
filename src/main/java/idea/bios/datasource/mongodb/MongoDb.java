package idea.bios.datasource.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import idea.bios.config.GlobalConfig;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.Document;

/**
 * 连接MongoDB
 * @author 86153
 */
public class MongoDb {
    private static final MongoClient MONGO_CLIENT = new MongoClient(
            GlobalConfig.getMongoBo().getHost(), GlobalConfig.getMongoBo().getPort());

    private static final MongoCollection<Document> FILTER_COLLECTION = MONGO_CLIENT.getDatabase(
            GlobalConfig.getMongoBo().getDatabase()).getCollection(
                    "Tools.Filter");

    @Deprecated
    public static synchronized boolean filter(String url, boolean addToFilterList) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String md5 = DigestUtils.md5Hex(url);
        // TODO 如果分布式爬虫，则使用分布式锁
        Document doc = FILTER_COLLECTION.find(Filters.eq("_id", md5)).first();
        if (doc == null || doc.isEmpty()) {
            // 不存在
            if (addToFilterList) {
                FILTER_COLLECTION.insertOne(new Document("_id", md5));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取存放data的collection
     * @param colName   colName
     */
    public synchronized MongoCollection<Document> getCrawlerDataCollection(String colName) {
        if (colName == null || colName.isEmpty()) {
            return null;
        }
        return MONGO_CLIENT.getDatabase(GlobalConfig.getMongoBo().getDatabase())
                .getCollection(colName);
    }

}
