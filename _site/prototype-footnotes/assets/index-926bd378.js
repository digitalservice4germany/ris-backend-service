import{d as m,c as o,F as d,r as _,b as p,e as g,a as s,f as l,g as u,w as f,h,t as k,i as x,j as v,k as y,T as N,l as w,u as C}from"./index-a65e9a75.js";import{g as T}from"./operations-7d5bef41.js";import"./dayjs.min-633259b2.js";const b={key:0,class:"pl-64"},B={class:"mb-24"},V=m({__name:"NormsList",props:{norms:null},setup(r){function t(e){return e===void 0||e.length<=0?"Kein Titel":e}return(e,n)=>{const i=g("router-link");return r.norms.length?(s(),o("div",b,[(s(!0),o(d,null,_(r.norms,a=>(s(),o("div",{key:a.guid},[l("div",B,[u(i,{class:"heading-03-regular",to:{name:"norms-norm-:normGuid",params:{normGuid:a.guid}}},{default:f(()=>[h(k(t(a.officialLongTitle)),1)]),_:2},1032,["to"])])]))),128))])):p("",!0)}}}),$={class:"bg-gray-100 flex flex-col gap-16"},D={class:"flex justify-between p-16 pl-64"},L=l("h1",{class:"heading-02-regular"},"Dokumentationseinheiten",-1),j={key:1,class:"pl-64 pt-[3.5rem]"},K=m({__name:"index",async setup(r){let t,e;const n=x(([t,e]=v(()=>T()),t=await t,e(),t).data),i=C();return(a,c)=>(s(),o("div",$,[l("div",D,[L,u(N,{label:"Neue Dokumentationseinheit",onClick:c[0]||(c[0]=A=>y(i).push({name:"norms-import"}))})]),n.value&&n.value.length!==0?(s(),w(V,{key:0,class:"grow max-w-screen-lg",norms:n.value},null,8,["norms"])):(s(),o("span",j,"Keine Normen gefunden"))]))}});export{K as default};
