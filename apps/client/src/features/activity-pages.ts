// 活动专属页面覆盖映射：键是活动 id，值是 pages.json 里注册的页面路径。
// 没配的活动统一落到通用 activity-event 页；要新增专属页，先在 pages.json 注册再在这里加一行。
export const ACTIVITY_PAGES: Record<string,string> = {
  '3662748781189241907': 'activity-event',
}
