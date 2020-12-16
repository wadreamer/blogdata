package club.wadreamer.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: RedisUtils
 * Description: TODO Redis 工具类
 * date: 2020/12/15 16:56
 *
 * @author CFG
 * @since JDK 1.8
 */
@Component
@SuppressWarnings({"unchecked", "all"})
public class RedisUtils {

    // redis 配置文件中自动注入该 redisTemplate
    private RedisTemplate<Object, Object> redisTemplate;

    public RedisUtils(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // =================================== key 操作 ===================================

    /**
     * @param key
     * @param time
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:24
     * @description: TODO 设置 key 的过期时间
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * @param key
     * @param time
     * @param timeUnit
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:25
     * @description: TODO 指定时间单位，设置 key 的过期时间
     */
    public boolean expire(String key, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, timeUnit);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * @param key
     * @return {{@link long}}
     * @author wadreamer
     * @date: 2020/12/16 14:30
     * @description: TODO 获取过期时间
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * @param key
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:31
     * @description: TODO 是否含有该 key
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param keys
     * @return {}
     * @author wadreamer
     * @date: 2020/12/16 14:31
     * @description: TODO 删除一个或多个 key
     */
    public void del(String... keys) {
        try {
            if (Objects.nonNull(keys) && keys.length > 0) {
                if (keys.length == 1) {
                    redisTemplate.delete(keys[0]);
                } else {
                    // 使用 set 去除重复的 key
                    HashSet<Object> keySet = new HashSet<>();
                    for (String key : keys) {
                        keySet.addAll(redisTemplate.keys(key));
                    }
                    Long count = redisTemplate.delete(keySet);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * @param prefix
     * @param ids
     * @return {}
     * @author wadreamer
     * @date: 2020/12/16 14:38
     * @description: TODO 删除以 prefix + id 为前缀的 key
     */
    public void delByKeys(String prefix, Set<Long> ids) {
        Set<Object> keys = new HashSet<>();

        for (Long id : ids) {
            keys.addAll(redisTemplate.keys(new StringBuffer(prefix).append(id).toString()));
        }
        redisTemplate.delete(keys);
    }

    /**
     * @param pattern
     * @return {{@link List< String>}}
     * @author wadreamer
     * @date: 2020/12/16 14:34
     * @description: TODO 查找匹配 key
     */
    public List<String> matchkey(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        RedisClusterConnection rc = Objects.requireNonNull(factory).getClusterConnection();

        Cursor<byte[]> cursor = rc.scan(options);
        List<String> result = new ArrayList<>();
        while (cursor.hasNext()) {
            result.add(new String(cursor.next()));
        }

        try {
            RedisConnectionUtils.releaseConnection(rc, factory);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return result;
    }

    /**
     * @param patternKey
     * @param page
     * @param size
     * @return {{@link List< String>}}
     * @author wadreamer
     * @date: 2020/12/16 14:42
     * @description: TODO 分页查询 key
     */
    public List<String> findKeysForPage(String patternKey, int page, int size) {
        ScanOptions options = ScanOptions.scanOptions().match(patternKey).build();
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        RedisClusterConnection rc = Objects.requireNonNull(factory).getClusterConnection();

        Cursor<byte[]> cursor = rc.scan(options);
        List<String> result = new ArrayList<>();

        int tmpIndex = 0;
        int fromIndex = page * size;
        int endIndex = page * size + size;

        while (cursor.hasNext()) {
            if (tmpIndex >= fromIndex && tmpIndex < endIndex) {
                result.add(new String(cursor.next()));
                tmpIndex++;
                continue;
            }

            if (tmpIndex >= endIndex) {
                break;
            }

            tmpIndex++;
            cursor.next();
        }

        try {
            RedisConnectionUtils.releaseConnection(rc, factory);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    // =================================== String 操作 ===================================

    /**
     * @param key
     * @return {{@link Object}}
     * @author wadreamer
     * @date: 2020/12/16 14:42
     * @description: TODO 单个缓存获取
     */
    public Object get(String key) {
        return Objects.isNull(key) ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * @param key
     * @return {{@link List< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 14:42
     * @description: TODO 批量获取缓存
     */
    public List<Object> multiGet(List<String> key) {
        List<Object> list = redisTemplate.opsForValue().multiGet(Sets.newHashSet(key));
        ArrayList<Object> resultList = Lists.newArrayList();
        // Lambda 表达式
        Optional.ofNullable(list).ifPresent(e -> list.forEach(item -> Optional.ofNullable(item).ifPresent(resultList::add)));
        return resultList;
    }

    /**
     * @param key
     * @param value
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:42
     * @description: TODO 存入单个缓存
     */
    public boolean set(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param value
     * @param time
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:42
     * @description: TODO 存入单个缓存，并指定过期时间
     */
    public boolean set(String key, String value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param value
     * @param time
     * @param timeUnit
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 存入单个缓存，并指定过期时间和时间单位
     */
    public boolean set(String key, String value, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, timeUnit);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param value
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 16:16
     * @description: TODO 若不存在，则存入值
     */
    public boolean setIfAbsent(String key, String value) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // =================================== Map 操作 ===================================

    /**
     * @param key
     * @param item
     * @return {{@link Object}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 获取 map 类型的缓存
     */
    public Object hGet(String key, String item) {
        return (Objects.isNull(key) || Objects.isNull(item)) ? null : redisTemplate.opsForHash().get(key, item);
    }

    /**
     * @param key
     * @return {{@link Map< Object, Object>}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 批量获取 map 类型的缓存
     */
    public Map<Object, Object> hmGet(String key) {
        return Objects.isNull(key) ? null : redisTemplate.opsForHash().entries(key);
    }

    /**
     * @param key
     * @param item
     * @param value
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 存入 map 类型的缓存
     */
    public boolean hSet(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param item
     * @param value
     * @param time
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 存入 map 类型的缓存，并指定过期时间
     */
    public boolean hSet(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param map
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 批量存入 map 类型的缓存
     */
    public boolean hmSet(String key, HashMap<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param items
     * @return {}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 删除 map 类型的缓存
     */
    public void hDel(String key, Object... items) {
        redisTemplate.opsForHash().delete(key, items);
    }

    /**
     * @param key
     * @param item
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 判断以 item 为 key 的 map 类型的缓存是否存在
     */
    public boolean hHasKey(String key, String item) {
        return (Objects.isNull(key) || Objects.isNull(item)) ? null : redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * @param key
     * @param item
     * @param by
     * @return {{@link double}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 使 map 类型的缓存自增长指定的值
     */
    public double hIncr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * @param key
     * @param item
     * @param by
     * @return {{@link double}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 使 map 类型的缓存自递减指定的值
     */
    public double hDecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    // =================================== Set 操作 ===================================

    /**
     * @param key
     * @return {{@link Set< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 获取 set 类型的缓存
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param value
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 判断 set 类型的缓存中是否存在指定的值
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param values
     * @return {{@link long}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 存入 set 类型的缓存
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return 0;
        }
    }

    /**
     * @param key
     * @param time
     * @param values
     * @return {{@link long}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 存储 set 类型的缓存，并指定过期时间
     */
    public long sSet(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return 0;
        }
    }

    /**
     * @param key
     * @return {{@link long}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 获取 set 类型的缓存的大小
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return 0;
        }
    }

    /**
     * @param key
     * @param values
     * @return {{@link long}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 移除 set 类型的缓存中指定的值
     */
    public long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return 0;
        }
    }

    /**
     * @param key
     * @param anotherKey
     * @return {{@link Set< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 15:44
     * @description: TODO 获取两个 set 类型的缓存的交集
     */
    public Set<Object> sIntersect(String key, String anotherKey) {
        try {
            return redisTemplate.opsForSet().intersect(key, anotherKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param otherKeys
     * @return {{@link Set< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 15:45
     * @description: TODO 获取某个集合与多个集合的交集
     */
    public Set<Object> sIntersect(String key, Collection<String> otherKeys) {
        try {
            return redisTemplate.opsForSet().intersect(key, otherKeys);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param anotherKey
     * @param destKey
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 15:47
     * @description: TODO 将两个集合的交集存入到另一个集合中
     */
    public Long sIntersectAndStore(String key, String anotherKey, String destKey) {
        try {
            return redisTemplate.opsForSet().intersectAndStore(key, anotherKey, destKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param anotherKey
     * @param destKey
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 15:48
     * @description: TODO 将某个集合与多个集合的交集存储到另一个集合中
     */
    public Long sIntersectAndStore(String key, Collection<String> othersKey, String destKey) {
        try {
            return redisTemplate.opsForSet().intersectAndStore(key, othersKey, destKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param anotherKey
     * @return {{@link Set< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 15:50
     * @description: TODO 获取两个 set 类型的缓存的并集
     */
    public Set<Object> sUnion(String key, String anotherKey) {
        try {
            return redisTemplate.opsForSet().union(key, anotherKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param anotherKey
     * @return {{@link Set< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 15:51
     * @description: TODO 获取某个集合和多个集合的并集
     */
    public Set<Object> sUnion(String key, Collection<String> otherKeys) {
        try {
            return redisTemplate.opsForSet().union(key, otherKeys);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param anotherKey
     * @return {{@link Set< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 15:50
     * @description: TODO 获取两个 set 类型的缓存的并集，并存储到指定集合中
     */
    public Long sUnion(String key, String anotherKey, String destkey) {
        try {
            return redisTemplate.opsForSet().unionAndStore(key, anotherKey, destkey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param anotherKey
     * @param key
     * @return {{@link Set< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 15:51
     * @description: TODO 获取某个集合和多个集合的并集，并存储到指定集合中
     */
    public Long sUnion(String key, Collection<String> otherKeys, String destKey) {
        try {
            return redisTemplate.opsForSet().unionAndStore(key, otherKeys, destKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param anotherKey
     * @return {{@link Set< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 15:50
     * @description: TODO 获取两个 set 类型的缓存的差集
     */
    public Set<Object> sDifference(String key, String anotherKey) {
        try {
            return redisTemplate.opsForSet().difference(key, anotherKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param anotherKey
     * @return {{@link Set< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 15:51
     * @description: TODO 获取某个集合和多个集合的差集
     */
    public Set<Object> sDifference(String key, Collection<String> otherKeys) {
        try {
            return redisTemplate.opsForSet().difference(key, otherKeys);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param anotherKey
     * @return {{@link Set< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 15:50
     * @description: TODO 获取两个 set 类型的缓存的差集，并存储到指定集合中
     */
    public Long sDifference(String key, String anotherKey, String destkey) {
        try {
            return redisTemplate.opsForSet().differenceAndStore(key, anotherKey, destkey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param anotherKey
     * @param key
     * @return {{@link Set< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 15:51
     * @description: TODO 获取某个集合和多个集合的差集，并存储到指定集合中
     */
    public Long sDifference(String key, Collection<String> otherKeys, String destKey) {
        try {
            return redisTemplate.opsForSet().differenceAndStore(key, otherKeys, destKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    // =================================== zSet 操作 ===================================

    /**
     * @param key
     * @param value
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 16:01
     * @description: TODO 返回元素在集合的排名，有序集合是按照元素的score值由小到大排列
     */
    public Long zRank(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().rank(key, value);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param value
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 16:02
     * @description: TODO 返回元素在集合的排名,按元素的score值由大到小排列
     */
    public Long zReverseRank(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().reverseRank(key, value);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param start
     * @param end
     * @return {{@link Set< String>}}
     * @author wadreamer
     * @date: 2020/12/16 16:09
     * @description: TODO 获取集合指定位置内的元素，并按从小到大的排序
     */
    public Set<Object> zRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().range(key, start, end);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param start
     * @param end
     * @return {{@link Set< ZSetOperations.TypedTuple< Object>>}}
     * @author wadreamer
     * @date: 2020/12/16 16:11
     * @description: TODO 批量获取集合指定位置内的元素, 并且把 score 值也获取
     */
    public Set<ZSetOperations.TypedTuple<Object>> zRangeWithScores(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().rangeWithScores(key, start, end);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param min
     * @param max
     * @return {{@link Set< String>}}
     * @author wadreamer
     * @date: 2020/12/16 16:13
     * @description: TODO 根据 score 获取指定范围内的集合元素，并按从小到大排序
     */
    public Set<Object> zRangeByScore(String key, double min, double max) {
        try {
            return redisTemplate.opsForZSet().rangeByScore(key, min, max);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param min
     * @param max
     * @return {{@link Set<TypedTuple<String>>}}
     * @author wadreamer
     * @date: 2020/12/16 16:14
     * @description: TODO 根据 score 获取指定范围内的集合元素和 score，并按从小到大排序，
     */
    public Set<ZSetOperations.TypedTuple<Object>> zRangeByScoreWithScores(String key, double min, double max) {
        try {
            return redisTemplate.opsForZSet().rangeByScoreWithScores(key, min, max);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param min
     * @param max
     * @param start
     * @param end
     * @return {{@link Set<TypedTuple<String>>}}
     * @author wadreamer
     * @date: 2020/12/16 16:21
     * @description: TODO 根据 score 和 index 获取指定范围内的集合元和 score，并按从小到大排序
     */
    public Set<ZSetOperations.TypedTuple<Object>> zRangeByScoreWithScores(String key, double min, double max, long start, long end) {
        try {
            return redisTemplate.opsForZSet().rangeByScoreWithScores(key, min, max, start, end);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param start
     * @param end
     * @return {{@link Set< String>}}
     * @author wadreamer
     * @date: 2020/12/16 16:24
     * @description: TODO 获取集合元素，按从大到小排序
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().reverseRange(key, start, end);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param start
     * @param end
     * @return {{@link Set<TypedTuple<String>>}}
     * @author wadreamer
     * @date: 2020/12/16 16:25
     * @description: TODO 获取集合的元素, 从大到小排序, 并返回 score 值
     */
    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param min
     * @param max
     * @return {{@link Set< String>}}
     * @author wadreamer
     * @date: 2020/12/16 16:26
     * @description: TODO 根据 score 值查询集合元素, 从大到小排序
     */
    public Set<Object> zReverseRangeByScore(String key, double min, double max) {
        try {
            return redisTemplate.opsForZSet().reverseRangeByScore(key, min, max);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param min
     * @param max
     * @return {{@link Set<TypedTuple<String>>}}
     * @author wadreamer
     * @date: 2020/12/16 16:28
     * @description: TODO
     */
    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeByScoreWithScores(String key, double min, double max) {
        try {
            return redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, min, max);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param min
     * @param max
     * @param start
     * @param end
     * @return {{@link Set< String>}}
     * @author wadreamer
     * @date: 2020/12/16 16:28
     * @description: TODO 根据 Score 和 index 查询集合元素, 从大到小排序
     */
    public Set<Object> zReverseRangeByScore(String key, double min, double max, long start, long end) {
        try {
            return redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, start, end);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 16:30
     * @description: TODO 获取集合元素的数量
     */
    public Long zSize(String key) {
        try {
            return redisTemplate.opsForZSet().size(key);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 16:30
     * @description: TODO 获取集合大小
     */
    public Long zZCard(String key) {
        try {
            return redisTemplate.opsForZSet().zCard(key);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param value
     * @return {{@link Double}}
     * @author wadreamer
     * @date: 2020/12/16 16:30
     * @description: TODO 获取集合中 value 元素的 score 值
     */
    public Double zScore(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().score(key, value);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * 根据score值获取集合元素数量
     *
     * @param key
     * @param min
     * @param max
     * @return
     */
    public Long zCount(String key, double min, double max) {
        return redisTemplate.opsForZSet().count(key, min, max);
    }

    /**
     * @param key
     * @param value
     * @param score
     * @return {{@link Boolean}}
     * @author wadreamer
     * @date: 2020/12/16 16:04
     * @description: TODO 添加元素，有序集合是按照元素的 score 值由小到大排列
     */
    public Boolean zAdd(String key, String value, double score) {
        try {
            return redisTemplate.opsForZSet().add(key, value, score);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param values
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 16:07
     * @description: TODO 批量添加元素，有序集合是按照元素的 score 值由小到大排列
     */
    public Long zAdd(String key, Set<ZSetOperations.TypedTuple<Object>> values) {
        try {
            return redisTemplate.opsForZSet().add(key, values);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param values
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 16:07
     * @description: TODO 批量移除 zSet 中指定的值
     */
    public Long zRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForZSet().remove(key, values);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param value
     * @param delta
     * @return {{@link Double}}
     * @author wadreamer
     * @date: 2020/12/16 16:08
     * @description: TODO 增加元素的 score 值，并返回增加后的值
     */
    public Double zIncrementScore(String key, String value, double delta) {
        try {
            return redisTemplate.opsForZSet().incrementScore(key, value, delta);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param start
     * @param end
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 16:34
     * @description: TODO 移除指定索引位置的元素
     */
    public Long zRemoveRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().removeRange(key, start, end);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param min
     * @param max
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 16:34
     * @description: TODO 根据指定的 score 值的范围来移除成员
     */
    public Long zRemoveRangeByScore(String key, double min, double max) {
        try {
            return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param otherKey
     * @param destKey
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 16:35
     * @description: TODO 获取两个指定集合的并集，并存储在指定集合中
     */
    public Long zUnionAndStore(String key, String otherKey, String destKey) {
        try {
            return redisTemplate.opsForZSet().unionAndStore(key, otherKey, destKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param otherKeys
     * @param destKey
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 16:36
     * @description: TODO 获取指定集合和多个集合的并集，并存储在指定集合中
     */
    public Long zUnionAndStore(String key, Collection<String> otherKeys, String destKey) {
        try {
            return redisTemplate.opsForZSet().unionAndStore(key, otherKeys, destKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param otherKey
     * @param destKey
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 16:37
     * @description: TODO 获取两个指定集合的交集，并存储在指定集合中
     */
    public Long zIntersectAndStore(String key, String otherKey, String destKey) {
        try {
            return redisTemplate.opsForZSet().intersectAndStore(key, otherKey, destKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param otherKeys
     * @param destKey
     * @return {{@link Long}}
     * @author wadreamer
     * @date: 2020/12/16 16:38
     * @description: TODO 获取指定集合和多个集合的交集，并存储在指定集合中
     */
    public Long zIntersectAndStore(String key, Collection<String> otherKeys, String destKey) {
        try {
            return redisTemplate.opsForZSet().intersectAndStore(key, otherKeys, destKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    // =================================== list 操作 ===================================

    /**
     * @param key
     * @param start
     * @param end
     * @return {{@link List< Object>}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 获取 list 类型的缓存中指定范围内的值
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @return {{@link long}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 获取 list 类型的缓存的大小
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return 0;
        }
    }

    /**
     * @param key
     * @param index
     * @return {{@link Object}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 获取 list 类型的缓存中的指定位置的值
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     * @param value
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 从右边往 list 类型缓存中存入指定的值
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param value
     * @param time
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 从右边往 list 类型缓存中存入指定的值，并指定过期时间
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param value
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 从右边往 list 类型缓存中存入 list 类型的值
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param value
     * @param time
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:43
     * @description: TODO 从右边往 list 类型缓存中存入 list 类型的值，并指定过期时间
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param index
     * @param value
     * @return {{@link boolean}}
     * @author wadreamer
     * @date: 2020/12/16 14:44
     * @description: TODO 修改 list 类型缓存中指定位置的值
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param count
     * @param value
     * @return {{@link long}}
     * @author wadreamer
     * @date: 2020/12/16 14:44
     * @description: TODO 移除 list 类型的缓存中指定数量的值
     */
    public long lRemove(String key, long count, Object value) {
        try {
            return redisTemplate.opsForList().remove(key, count, value);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return 0;
        }
    }

}
