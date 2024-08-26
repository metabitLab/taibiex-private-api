package com.metabitlab.taibiex.privateapi.fetcher;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.DgsConstants;
import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.InputArgument;

import java.util.List;

@DgsComponent
public class NftCollectionDataFetcher {

    @DgsData(parentType = DgsConstants.QUERY.TYPE_NAME)
    public NftCollectionConnection topCollections(@InputArgument List<Chain> chains,
                                                  @InputArgument CollectionSortableField orderBy,
                                                  @InputArgument HistoryDuration duration,
                                                  @InputArgument String after,
                                                  @InputArgument Integer first,
                                                  @InputArgument String cursor,
                                                  @InputArgument Integer limit
                                                  ) {
        return null;
    }

    @DgsData(parentType = DgsConstants.QUERY.TYPE_NAME)
    public NftCollectionConnection nftCollections(@InputArgument Chain chain,
                                                  @InputArgument NftCollectionsFilterInput filter,
                                                  @InputArgument String after,
                                                  @InputArgument Integer first) {
        return null;
    }

}
