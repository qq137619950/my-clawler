package idea.bios.crawler.my.seed;

import idea.bios.datasource.mysql.OmahaMapper;
import idea.bios.datasource.mysql.dao.SeedDao;
import idea.bios.util.search.BaiduSfSearchLinks;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author 86153
 */
@Slf4j
public class SeedFetcherImpl implements SeedFetcher {
    private static final String MYBATIS_CFG_PATH = "mybatis-config.xml";
    @Override
    public List<String> getSeedsPlain(String... urls) {
        if (urls.length == 0) {
            return new ArrayList<>();
        }
        return Arrays.asList(urls);
    }

    @Override
    public List<String> getSeedsFromDb(int offset, int limit,
                                       Function<String, String> urlConvert) {
        InputStream inputStream;
        try {
            inputStream = Resources.getResourceAsStream(MYBATIS_CFG_PATH);
        } catch (IOException e) {
            log.warn("Exception occur", e);
            return new ArrayList<>();
        }
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            OmahaMapper mapper = session.getMapper(OmahaMapper.class);
            List<String> terms = mapper.batchGetBiosChiShortTerm(offset, limit);
            if (terms == null || terms.isEmpty()) {
                return new ArrayList<>();
            }
            return terms.stream().map(urlConvert).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Exception occur", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getSeedsFromTxt(String filePath) {
        // TODO
        return null;
    }

    @Override
    public List<String> getSeedsFromPool(String sourceId) {
        log.info("get seeds from pool. sourceId:{}", sourceId);
        InputStream inputStream;
        try {
            inputStream = Resources.getResourceAsStream(MYBATIS_CFG_PATH);
        } catch (IOException e) {
            log.warn("Exception occur", e);
            return new ArrayList<>();
        }
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            OmahaMapper mapper = session.getMapper(OmahaMapper.class);
            List<SeedDao> seeds = mapper.batchGetSeeds(sourceId);
            if (seeds == null || seeds.isEmpty()) {
                return new ArrayList<>();
            }
            // 删除
            mapper.delSeedsByIds(seeds.stream().map(SeedDao::getId)
                    .collect(Collectors.toList()));
            return seeds.stream().map(SeedDao::getSite)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Exception occur", e);
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        System.out.println(new SeedFetcherImpl().getSeedsFromDb(
                0, 1, term -> BaiduSfSearchLinks.URL_PREFIX + term));
    }
}
