package com.sudeng.zhangben.util

import com.sudeng.zhangben.data.local.entity.CategoryEntity
import com.sudeng.zhangben.data.local.entity.CategoryType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantClassifier @Inject constructor() {

    private val defaultRules: Map<String, List<String>> = mapOf(
        "餐饮" to listOf(
            "美团", "饿了么", "星巴克", "麦当劳", "肯德基", "海底捞",
            "瑞幸", "茶颜悦色", "喜茶", "奈雪", "蜜雪冰城", "必胜客",
            "汉堡王", "华莱士", "呷哺", "太二", "西贝", "外婆家",
            "食堂", "餐厅", "饭店", "面馆", "小吃", "烧烤", "火锅",
            "麻辣烫", "米线", "饺子", "包子", "外卖", "食品", "水果",
            "超市", "便利店", "罗森", "全家", "711", "永辉", "盒马",
            "买菜", "叮咚", "朴朴", "每日优鲜", "菜市场"
        ),
        "交通" to listOf(
            "滴滴", "曹操", "T3", "花小猪", "首汽", "神州",
            "地铁", "公交", "高铁", "火车", "机票", "航班",
            "加油", "充电", "停车", "高速", "ETC", "交管",
            "哈啰", "美团单车", "青桔", "摩拜", "打车", "出租车",
            "汽车", "车票", "12306"
        ),
        "购物" to listOf(
            "淘宝", "天猫", "京东", "拼多多", "唯品会", "苏宁",
            "闲鱼", "当当", "得物", "小红书", "抖音商城",
            "服装", "衣服", "鞋", "包", "化妆品", "护肤品",
            "数码", "手机", "电脑", "电器", "家电", "家具",
            "百货", "商场", "购物中心", "屈臣氏", "名创", "无印"
        ),
        "娱乐" to listOf(
            "电影院", "猫眼", "淘票票", "KTV", "网吧", "游戏",
            "哔哩哔哩", "B站", "腾讯视频", "爱奇艺", "优酷", "芒果TV",
            "网易云", "QQ音乐", "酷狗", "抖音", "快手",
            "旅行", "酒店", "民宿", "景点", "门票", "携程", "去哪儿",
            "飞猪", "马蜂窝", "迪士尼", "欢乐谷"
        ),
        "居家" to listOf(
            "水电", "水费", "电费", "燃气", "天然气", "暖气",
            "物业", "房租", "房贷", "租金",
            "搬家", "保洁", "维修", "装修", "家政",
            "快递", "顺丰", "圆通", "中通", "申通", "韵达", "京东物流",
            "宽带", "电信", "联通", "移动"
        ),
        "医疗" to listOf(
            "医院", "诊所", "药房", "药店", "医保",
            "体检", "挂号", "门诊", "丁香园", "春雨",
            "牙科", "眼科", "中医", "体检中心"
        ),
        "教育" to listOf(
            "学费", "培训", "课程", "书本", "教材",
            "学而思", "新东方", "作业帮", "猿辅导", "得到",
            "知乎", "豆瓣", "Kindle", "微信读书", "考试"
        ),
        "通讯" to listOf(
            "话费", "流量", "宽带", "中国移动", "中国联通", "中国电信",
            "充值", "套餐"
        ),
        "工资" to listOf(
            "工资", "薪资", "薪酬", "劳务", "报酬", "补贴", "津贴",
            "公积金", "社保", "报销", "退税"
        ),
        "奖金" to listOf(
            "奖金", "年终奖", "绩效", "分红", "提成"
        ),
        "投资" to listOf(
            "理财", "基金", "股票", "余额宝", "零钱通", "收益",
            "利息", "分红", "红利"
        ),
        "其他收入" to listOf(
            "红包", "转账", "退款", "返利", "中奖"
        )
    )

    fun classify(merchantName: String, categories: List<CategoryEntity>): Long? {
        if (merchantName.isBlank()) return null

        val name = merchantName.trim()

        for ((categoryName, keywords) in defaultRules) {
            for (keyword in keywords) {
                if (name.contains(keyword, ignoreCase = true)) {
                    val cat = categories.find { it.name == categoryName && it.type == CategoryType.EXPENSE }
                        ?: categories.find { it.name == categoryName && it.type == CategoryType.INCOME }
                    return cat?.id
                }
            }
        }

        return null
    }
}
