package it.polimi.nsds.spark.lab.friends;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;

/**
 * Implement an iterative algorithm that implements the transitive closure of friends
 * (people that are friends of friends of ... of my friends).
 *
 * Set the value of the flag useCache to see the effects of caching.
 */
public class FriendsComputation {
    private static final boolean useCache = true;

    public static void main(String[] args) {
        final String master = args.length > 0 ? args[0] : "local[4]";
        final String filePath = args.length > 1 ? args[1] : ".\\";
        final String appName = useCache ? "FriendsCache" : "FriendsNoCache";

        final SparkSession spark = SparkSession
                .builder()
                .master(master)
                .appName(appName)
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        final List<StructField> fields = new ArrayList<>();
        fields.add(DataTypes.createStructField("person", DataTypes.StringType, false));
        fields.add(DataTypes.createStructField("friend", DataTypes.StringType, false));
        final StructType schema = DataTypes.createStructType(fields);

        final Dataset<Row> input = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(schema)
                .csv(filePath + "files\\friends\\friends.csv");


        boolean modified = true;
        Dataset<Row> friends = input;

        while(modified) {
            Dataset<Row> friends1 = friends.withColumnRenamed("friend", "to-join");
            Dataset<Row> friends2 = friends.withColumnRenamed("person", "to-join");
           
            //We compute the join of the two table (one level transitivity)
            Dataset<Row> newFriends = friends1
                    .join(friends2, "to-join")
                    .select("person", "friend")
                    .distinct();
            
            //We check if the new table is different from the previous one
            modified = newFriends.except(friends).count() > 0;

            //We update the friends table
            friends = friends.union(newFriends).distinct();

            //We cache the friends table
            if (useCache) {
                friends.cache();
            }
        }

        friends.show();

        spark.close();
    }
}
