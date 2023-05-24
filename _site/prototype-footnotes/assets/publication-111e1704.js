import{_ as H}from"./DocumentUnitWrapper.vue_vue_type_script_setup_true_lang-82b9e759.js";import{d as U,i as h,M as O,a as l,c as o,f as e,t as m,F as z,r as L,aa as Y,V as q,A as P,k as p,h as A,b as M,g as w,w as C,T as I,a6 as Z,N as J,O as K,l as k,L as B,Z as T,z as Q,X as W,$ as ee,a0 as te,j as se}from"./index-f3f720fc.js";import{f as le}from"./coreDataFields-05f1c912.js";import{_ as j,I as ne}from"./InfoModal.vue_vue_type_script_setup_true_lang-d4e377a9.js";import{d as ie}from"./documentUnitService-1fe62a03.js";import"./dayjs.min-633259b2.js";import"./SideToggle.vue_vue_type_script_setup_true_lang-499c8b7f.js";import"./types-7b85aa56.js";const ae={court:"Gericht",date:"Entscheidungsdatum",fileNumber:"Aktenzeichen",documentType:"Dokumenttyp"},oe={class:"flex flex-col gap-24 text-base xml-container"},re={class:"label-03-bold"},ue={class:"border-1 border-solid border-white code-lines"},ce={class:"line"},de=U({__name:"CodeSnippet",props:{title:null,xml:null},setup(n){const i=n,t=()=>i.xml.includes("<?xml")?i.xml.split(`
`).filter(r=>r.length>0):[],s=h(t());return O(()=>i.xml,()=>{s.value=t()}),(r,f)=>(l(),o("div",oe,[e("p",re,m(n.title),1),e("div",ue,[(l(!0),o(z,null,L(s.value,(b,_)=>(l(),o("div",{key:_,class:"code-line"},[e("code",{class:"line-number",style:Y({"min-width":`${s.value.length.toString().length*15}px`})},[e("span",null,m(_+1),1)],4),e("code",ce,[e("span",null,m(b),1)])]))),128))])]))}});const me=q(de,[["__scopeId","data-v-c5d365a9"]]),he={class:"flex flex-col flex-start gap-40 justify-start max-w-[42rem]"},fe=e("h1",{class:"heading-02-regular"},"Veröffentlichen",-1),_e={"aria-label":"Plausibilitätsprüfung",class:"flex flex-row gap-16"},pe=e("div",{class:"w-[15.625rem]"},[e("p",{class:"subheading"},"1. Plausibilitätsprüfung")],-1),be={key:0,class:"flex flex-row gap-8"},ge=e("div",null,[e("span",{class:"bg-red-800 material-icons rounded-full text-white"}," error ")],-1),ve={class:"flex flex-col gap-32"},xe=e("p",{class:"body-01-reg"}," Die folgenden Rubriken-Pflichtfelder sind nicht befüllt: ",-1),ye={class:"list-disc"},De={key:0,class:"body-01-reg list-item ml-[1rem]"},ke={key:0},Me={class:"label-02-bold"},we={key:1,class:"flex flex-row gap-8"},Ee=e("span",{class:"material-icons text-green-700"}," check ",-1),$e=e("p",{class:"body-01-reg"},"Alle Pflichtfelder sind korrekt ausgefüllt",-1),Pe=[Ee,$e],Ae=e("div",{class:"border-b-1 border-b-gray-400"},null,-1),Ue={class:"flex flex-row gap-16"},ze=e("div",{class:"w-[15.625rem]"},[e("p",{class:"subheading"},"2. Empfänger der Export-Email")],-1),Le={class:"grow"},Ce=e("div",{class:"border-b-1 border-b-gray-400"},null,-1),Fe={"aria-label":"Letzte Veröffentlichungen",class:"flex flex-col gap-24"},Se=e("h2",{class:"heading-03-regular"},"Letzte Veröffentlichungen",-1),Ve={key:0},Ne={key:1,class:"flex flex-col gap-24"},Xe={class:"label-02-regular"},Re=e("div",{class:"label-section text-gray-900"},"ÜBER",-1),Ie={class:"label-02-regular"},Be=e("span",{class:"label-02-bold"},"E-Mail an:",-1),Te=e("span",{class:"label-02-bold"}," Betreff: ",-1),je=e("div",{class:"label-section text-gray-900"},"ALS",-1),Ge=U({__name:"PublicationDocument",props:{documentUnit:null,publishResult:null,lastPublishedXmlMail:null,errorMessage:null,succeedMessage:null},emits:["publishADocument"],setup(n,{emit:i}){var F;const t=n,s=P(()=>({name:"caselaw-documentUnit-:documentNumber-categories",params:{documentNumber:t.documentUnit.documentNumber}})),r=h("dokmbx@juris.de"),f=h(!1),b=P(()=>!t.lastPublishedXmlMail),_=h(),x=P(()=>_.value??t.errorMessage);function u(){E.value&&(_.value={title:"Es sind noch nicht alle Pflichtfelder befüllt.",description:"Die Dokumentationseinheit kann nicht veröffentlicht werden."}),a()?(f.value=!1,i("publishADocument",r.value)):f.value=!0}function a(){return/(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])/.test(r.value)}function g(c){c.target.select()}const y=h(t.documentUnit.missingRequiredFields.map(c=>le[c])),v=h((F=t.documentUnit.proceedingDecisions)==null?void 0:F.filter(c=>$(c).length>0).map(c=>({identifier:c.renderDecision,missingFields:$(c)}))),E=P(()=>{var c;return!!(y.value.length||(c=v.value)!=null&&c.length)});function $(c){return c.missingRequiredFields.map(D=>ae[D])}return(c,D)=>{var S,V,N,X,R;return l(),o("div",he,[fe,e("div",_e,[pe,p(E)?(l(),o("div",be,[ge,e("div",ve,[e("div",null,[xe,e("ul",ye,[(l(!0),o(z,null,L(y.value,d=>(l(),o("li",{key:d,class:"body-01-reg list-item ml-[1rem]"},m(d),1))),128)),v.value&&v.value.length>0?(l(),o("li",De,[A(" Rechtszug "),e("ul",null,[(l(!0),o(z,null,L(v.value,d=>(l(),o("li",{key:v.value.indexOf(d),class:"body-01-reg list-item ml-[1rem]"},[d&&d.missingFields.length>0?(l(),o("div",ke,[e("span",null,m(d.identifier),1),A(" - "),e("span",Me,m(d.missingFields.join(", ")),1)])):M("",!0)]))),128))])])):M("",!0)])]),w(p(Z),{to:p(s)},{default:C(()=>[w(I,{"aria-label":"Rubriken bearbeiten","button-type":"tertiary",class:"w-fit",label:"Rubriken bearbeiten"})]),_:1},8,["to"])])])):(l(),o("div",we,Pe))]),Ae,e("div",Ue,[ze,e("div",Le,[w(K,{id:"receiverAddress",key:"receiverAddress","error-message":f.value?"E-Mail-Adresse ungültig":void 0,label:"Empfänger-E-Mail-Adresse:"},{default:C(()=>[w(J,{id:"receiverAddress",modelValue:r.value,"onUpdate:modelValue":D[0]||(D[0]=d=>r.value=d),"aria-label":"Empfängeradresse E-Mail",onFocus:D[1]||(D[1]=d=>g(d))},null,8,["modelValue"])]),_:1},8,["error-message"])])]),Ce,p(x)?(l(),k(j,B({key:0,"aria-label":"Fehler bei Veröffentlichung",class:"mt-8"},p(x)),null,16)):M("",!0),n.succeedMessage?(l(),k(j,B({key:1,"aria-label":"Erfolg der Veröffentlichung",class:"mt-8"},n.succeedMessage,{status:p(ne).SUCCEED}),null,16,["status"])):M("",!0),w(I,{"aria-label":"Dokumentationseinheit veröffentlichen","button-type":"secondary",class:"w-fit",icon:"campaign",label:"Dokumentationseinheit veröffentlichen",onClick:u}),e("div",Fe,[Se,p(b)?(l(),o("p",Ve," Diese Dokumentationseinheit wurde bisher nicht veröffentlicht ")):(l(),o("div",Ne,[e("div",Xe," Letzte Veröffentlichung am "+m((S=t.lastPublishedXmlMail)==null?void 0:S.publishDate)+" (Zustellung: "+m((V=t.lastPublishedXmlMail)==null?void 0:V.publishStateDisplayText)+") ",1),Re,e("div",Ie,[e("div",null,[Be,A(" "+m((N=t.lastPublishedXmlMail)==null?void 0:N.receiverAddress),1)]),e("div",null,[Te,A(" "+m((X=t.lastPublishedXmlMail)==null?void 0:X.mailSubject),1)])]),je,(R=t.lastPublishedXmlMail)!=null&&R.xml?(l(),k(me,{key:0,title:"XML",xml:t.lastPublishedXmlMail.xml},null,8,["xml"])):M("",!0)]))])])}}}),qe={async publishDocument(n,i){var s,r;const t=await T.put(`caselaw/documentunits/${n}/publish`,{headers:{"Content-Type":"text/plain"}},i);return(t.status>=300||Number((s=t.data)==null?void 0:s.statusCode)>=300)&&(t.status=(r=t.data)!=null&&r.statusCode?Number(t.data.statusCode):t.status,t.error={title:"Leider ist ein Fehler aufgetreten.",description:"Die Dokumentationseinheit kann nicht veröffentlicht werden."}),t},async getLastPublishedXML(n){const i=await T.get(`caselaw/documentunits/${n}/publish`);return i.error=i.status>=300?{title:"Fehler beim Laden der letzten Veröffentlichung",description:"Die Daten der letzten Veröffentlichung konnten nicht geladen werden."}:void 0,i}},G=qe,He=n=>(ee("data-v-0cf163bb"),n=n(),te(),n),Oe={key:1,class:"spinner"},Ye=He(()=>e("h2",null,"Überprüfung der Daten ...",-1)),Ze=[Ye],Je=U({__name:"DocumentUnitPublication",props:{documentUnit:null},setup(n){const i=n,t=h(!1),s=h(),r=h(),f=h(),b=h();async function _(u){var g;const a=await G.publishDocument(i.documentUnit.uuid,u);r.value=a.data,a.data&&Number((g=a.data)==null?void 0:g.statusCode)<300?(s.value=a.data,s.value.publishDate=x(s.value.publishDate),s.value.xml=s.value.xml?s.value.xml.replace(/[ \t]{2,}/g,""):"",b.value={title:"Email wurde versendet",description:""}):f.value=a.error}function x(u){if(!u)return"";const a=new Date(u),g=a.getFullYear(),y=("0"+(a.getMonth()+1)).slice(-2),v=("0"+a.getDate()).slice(-2),E=("0"+a.getHours()).slice(-2),$=("0"+a.getMinutes()).slice(-2);return`${v}.${y}.${g} um ${E}:${$} Uhr`}return Q(async()=>{const u=await G.getLastPublishedXML(i.documentUnit.uuid);if(u.error){t.value=!0;return}u.data&&(s.value=u.data),s.value&&(s.value.publishDate=x(s.value.publishDate),s.value.xml=s.value.xml?s.value.xml:""),f.value=u.error,t.value=!0}),(u,a)=>(l(),k(H,{"document-unit":n.documentUnit},{default:C(({classes:g})=>[e("div",{class:W(g)},[t.value?(l(),k(Ge,{key:0,"document-unit":n.documentUnit,"error-message":f.value,"last-published-xml-mail":s.value,"publish-result":r.value,"succeed-message":b.value,onPublishADocument:a[0]||(a[0]=y=>_(y))},null,8,["document-unit","error-message","last-published-xml-mail","publish-result","succeed-message"])):(l(),o("div",Oe,Ze))],2)]),_:1},8,["document-unit"]))}});const Ke=q(Je,[["__scopeId","data-v-0cf163bb"]]),Qe={key:1},ot=U({__name:"publication",props:{documentNumber:null},async setup(n){let i,t;const s=n;async function r(){const _=await ie.getByDocumentNumber(s.documentNumber);return{documentUnit:h(_.data),error:_.error}}const{documentUnit:f,error:b}=([i,t]=se(()=>r()),i=await i,t(),i);return(_,x)=>{var u,a;return p(f)?(l(),k(Ke,{key:0,"document-unit":p(f)},null,8,["document-unit"])):(l(),o("div",Qe,[e("h2",null,m((u=p(b))==null?void 0:u.title),1),e("p",null,m((a=p(b))==null?void 0:a.description),1)]))}}});export{ot as default};
